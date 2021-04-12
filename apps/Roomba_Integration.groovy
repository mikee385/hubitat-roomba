/**
 *  Roomba Integration
 *
 *  Copyright 2019 Dominick Meglio
 *  Modified 2021 Michael Pierce
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
 
String getVersionNum() { return "1.0.0" }
String getVersionLabel() { return "Roomba Integration, version ${getVersionNum()} on ${getPlatform()}" }

definition(
    name: "Roomba Integration",
    namespace: "mikee385", 
	author: "Dominick Meglio and Michael Pierce", 
    description: "Connects to Roomba via Dorita",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    importUrl: "https://raw.githubusercontent.com/mikee385/hubitat-roomba/master/apps/Roomba_Integration.groovy")

preferences {
	page(name: "prefDorita", title: "Dorita Interface")
}

def prefDorita() {
	return dynamicPage(name: "prefDorita", title: "Connect to Dorita/Rest980", uninstall:true, install: true) {
		section("Dorita Information") {
			input("doritaIP", "text", title: "Dorita IP Address", description: "Dorita IP Address", required: true)
			input("doritaPort", "number", title: "Dorita Port", description: "Dorita Port", required: true, defaultValue: 3000, range: "1..65535")
		}
		section {
            input name: "alertOffline", type: "bool", title: "Alert when offline?", defaultValue: false
            input "offlineDuration", "number", title: "Minimum time before offline (in minutes)", required: true, defaultValue: 60
        }
        section {
            input "notifier", "capability.notification", title: "Notification Device", multiple: false, required: true
            input name: "logEnable", type: "bool", title: "Enable debug logging?", defaultValue: false
            label title: "Assign a name", required: true
        }
	}
}

def installed() {
	logDebug("Installed with settings: ${settings}")

	initialize()
}

def updated() {
	logDebug("Updated with settings: ${settings}")
    unschedule()
	initialize()
}

def uninstalled() {
	logDebug("Uninstalled app")

	for (device in getChildDevices())
	{
		deleteChildDevice(device.deviceNetworkId)
	}	
}

def initialize() {
	logDebug("initializing")

	cleanupChildDevices()
	createChildDevices()
    schedule("0/30 * * * * ? *", updateDevices)
    
    heartbeat()
}

def logDebug(msg) {
    if (logEnable) {
        log.debug msg
    }
}

def getDeviceNetworkId(data) {
	return "roomba:" + data.mac.replace(":", "")
}

def createChildDevices() {
    def result = executeAction("/api/local/info/state")
	if (result && result.data)
    {
        if (!getChildDevice(getDeviceNetworkId(result.data)))
            addChildDevice("mikee385", "Roomba", getDeviceNetworkId(result.data), 1234, ["name": result.data.name, isComponent: false])
    }
}

def cleanupChildDevices()
{
    def result = executeAction("/api/local/info/state")
    if (result && result.data)
    {
		for (device in getChildDevices())
		{
			if (getDeviceNetworkId(result.data) != device.deviceNetworkId)
            		deleteChildDevice(device.deviceNetworkId)
		}
	}
}

def updateDevices() {
    def result = executeAction("/api/local/info/state")
    if (result && result.data)
    {
        def device = getChildDevice(getDeviceNetworkId(result.data))
        
        device.sendEvent(name: "battery", value: result.data.batPct)
        if (!result.data.bin.present)
            device.sendEvent(name: "consumableStatus", value: "missing")
        else if (result.data.bin.full)
            device.sendEvent(name: "consumableStatus", value: "maintenance_required")
        else
            device.sendEvent(name: "consumableStatus", value: "good")
        
		def status = ""
		switch (result.data.cleanMissionStatus.phase)
		{
			case "hmMidMsn":
			case "hmPostMsn":
			case "hmUsrDock":
				status = "homing"
				break
			case "charge":
				status = "charging"
				break
			case "run":
				status = "cleaning"
				break
			case "stop":
				status = "idle"
				break		
		}
        device.sendEvent(name: "cleanStatus", value: status)
        
        heartbeat()
    } else {
    		state.healthStatus = "unhealthy"
    }
}

def handleStart(device, id) 
{
    def result = executeAction("/api/local/action/start")
    if (result && result.data && result.data.success == "null")
        device.sendEvent(name: "cleanStatus", value: "cleaning")
}

def handleStop(device, id) 
{
    def result = executeAction("/api/local/action/stop")
    if (result && result.data && result.data.success == "null")
        device.sendEvent(name: "cleanStatus", value: "idle")
}

def handlePause(device, id) 
{
    def result = executeAction("/api/local/action/pause")
    if (result && result.data && result.data.success == "null")
        device.sendEvent(name: "cleanStatus", value: "idle")
}

def handleResume(device, id) 
{
    def result = executeAction("/api/local/action/resume")
    if (result && result.data && result.data.success == "null")
        device.sendEvent(name: "cleanStatus", value: "cleaning")
}

def handleDock(device, id) 
{
    def result = executeAction("/api/local/action/dock")
	if (result && result.data && result.data.success == "null")
        device.sendEvent(name: "cleanStatus", value: "homing")
}

def executeAction(path) {
	def params = [
        uri: "http://${doritaIP}:${doritaPort}",
        path: "${path}",
		contentType: "application/json"
	]
	def result = null
	logDebug("calling action ${path}")
	try
	{
		httpGet(params) { resp ->
			result = resp
		}
	}
	catch (e) 
	{
		log.error("HTTP Exception Received: $e")
	}
	return result
}

def heartbeat() {
    unschedule("healthCheck")
    state.healthStatus = "online"
    runIn(60*offlineDuration, healthCheck)
}

def healthCheck() {
    state.healthStatus = "offline"
    if (alertOffline) {
    		notifier.deviceNotification("${getLabel()} is offline!")
    	}
}