/**
 *  Roomba Driver
 *
 *  Copyright 2019 Dominick Meglio
 *  Modified 2021-2022 Michael Pierce
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
 

String getVersionNum() { return "3.0.0" }
String getVersionLabel() { return "Roomba, version ${getVersionNum()} on ${getPlatform()}" }

metadata {
    definition (
		name: "Roomba", 
		namespace: "mikee385", 
		author: "Dominick Meglio and Michael Pierce", 
		importUrl: "https://raw.githubusercontent.com/mikee385/hubitat-roomba/master/drivers/Roomba.groovy"
	) {
	    capability "Actuator"
		capability "Battery"
        capability "Consumable"
		capability "Switch"
        
        attribute "cleanStatus", "string"
        
        attribute "phase", "string"
        attribute "cycle", "string"
        
        attribute "mssnStrtTm", "number"
        attribute "expireTm", "number"
        attribute "rechrgTm", "number"
        
        command "start"
        command "stop"
        command "pause"
        command "resume"
        command "dock"
    }
}

def installed() {
    initialize()
}

def uninstalled() {
    for (device in getChildDevices()) {
        deleteChildDevice(device.deviceNetworkId)
    }
}

def updated() {
    unschedule()
    initialize()
}

def initialize() {
    def startButton = childDevice("Start")
    def stopButton = childDevice("Stop")
    def pauseButton = childDevice("Pause")
    def resumeButton = childDevice("Resume")
    def dockButton = childDevice("Dock")
}

def childDevice(name) {
    def childID = "roomba:${device.getId()}:$name"
    def child = getChildDevice(childID)
    if (!child) {
        def childName = "${device.label ?: device.name}"
        child = addChildDevice("hubitat", "Generic Component Switch", childID, [label: "$childName $name", isComponent: true])
        child.updateSetting("logEnable", [value: "false", type: "bool"])
        child.updateSetting("txtEnable", [value: "false", type: "bool"])
        child.updateDataValue("Name", name)
        child.sendEvent(name: "switch", value: "off")
    }
    return child
}

def componentRefresh(cd) {}

def componentOn(cd) {
    def child = getChildDevice(cd.deviceNetworkId)
    child.sendEvent(name: "switch", value: "on")
    
    def name = child.getDataValue("Name")
    if (name == "Start") {
        start()
    } else if (name == "Stop") {
        stop()
    } else if (name == "Pause") {
        pause()
    } else if (name == "Resume") {
        resume()
    } else if (name == "Dock") {
        dock()
    } else {
        log.error "Unknown command name: $name"
    }
    
    runIn(1, componentOff, [data: [deviceNetworkId: cd.deviceNetworkId]])
}

def componentOff(cd) {
    def child = getChildDevice(cd.deviceNetworkId)
    child.sendEvent(name: "switch", value: "off")
}

def start() {
    parent.handleStart(device, device.deviceNetworkId.split(":")[1])
}

def stop() {
    parent.handleStop(device, device.deviceNetworkId.split(":")[1])
}

def pause() {
    parent.handlePause(device, device.deviceNetworkId.split(":")[1])
}

def resume() {
    parent.handleResume(device, device.deviceNetworkId.split(":")[1])
}

def dock() {
    parent.handleDock(device, device.deviceNetworkId.split(":")[1])
}

def on() {
    start()
}

def off() {
    stop()
}