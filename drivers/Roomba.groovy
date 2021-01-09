/**
 *  Roomba Driver
 *
 *  Copyright 2019 Dominick Meglio
 *  Modified by Michael Pierce
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
 

String getVersionNum() { return "1.0.0-beta.1" }
String getVersionLabel() { return "Roomba, version ${getVersionNum()} on ${getPlatform()}" }

metadata {
    definition (
		name: "Roomba", 
		namespace: "mikee385", 
		author: "Dominick Meglio and Michael Pierce", 
		importUrl: "https://raw.githubusercontent.com/mikee385/hubitat-roomba/master/drivers/Roomba.groovy"
	) {
		capability "Battery"
        capability "Consumable"
		capability "Actuator"
        
        attribute "cleanStatus", "string"
        
        command "start"
        command "stop"
        command "pause"
        command "resume"
        command "dock"
    }
}

def start() 
{
    parent.handleStart(device, device.deviceNetworkId.split(":")[1])
}

def stop() 
{
    parent.handleStop(device, device.deviceNetworkId.split(":")[1])
}

def pause() 
{
    parent.handlePause(device, device.deviceNetworkId.split(":")[1])
}

def resume() 
{
    parent.handleResume(device, device.deviceNetworkId.split(":")[1])
}

def dock() 
{
    parent.handleDock(device, device.deviceNetworkId.split(":")[1])
}