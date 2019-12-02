/**
 *  SmartThings Device Handler: Honeywell Partition
 *
 *  Author: tim.burris@chalklogic.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */
metadata {
  definition (name: "Honeywell VAM By Chalk Logic", namespace: "ChalkLogic", author: "Tim Burris") {
    capability "Button"
    //capability "Alarm"

    command "partition"
    //command "disarm"
    command "armInstant"
    command "armAway"
    command "refreshStatus"
  }
 	preferences {
        input "vamAddress", "text", title: "VAM Address", description: "(ie. 192.168.1.11)",required: true, displayDuringSetup: true
        input "vamPort", "text", title: "Host Port", description: "(ie. 80)", required: false, defaultValue: "80", displayDuringSetup: true
        input name: "userCode", type: "password", title: "User Code", description: "Enter alarm user code", required: true, displayDuringSetup: true
    }
  tiles(scale: 2) {
    multiAttributeTile(name:"partition", type: "generic", width: 6, height: 4){
      tileAttribute ("device.dscpartition", key: "PRIMARY_CONTROL") {
        attributeState "Unknown", label: 'Refresh Needed', backgroundColor: "#aab7b8", icon:"st.Home.home2"        
        attributeState "Ready To Arm", label: 'Disarmed', backgroundColor:"#239b56", icon:"st.Home.home2"
        attributeState "Armed Instant", label: 'Armed Instant', backgroundColor: "#a569bd", icon:"st.Home.home3"
        attributeState "Armed Stay", label: 'Armed', backgroundColor: "#e74c3c", icon:"st.Home.home3"
        attributeState "Armed Away", label: 'Armed', backgroundColor: "#e74c3c", icon:"st.Home.home3"
        attributeState "Not Ready Fault", label: 'Not Ready', backgroundColor: "#ffcc00", icon:"st.Home.home2"        
        attributeState "Refresh", label: 'Refreshing...', backgroundColor: "#aab7b8", icon:"st.Home.home2"        
        attributeState "Arming", label: 'Arming...', backgroundColor: "#f39c12", icon:"st.Home.home2"        
        attributeState "Sending Command", label: 'Sending Command...', backgroundColor: "#aab7b8", icon:"st.Home.home2"        
        //attributeState "armedaway", label: 'Armed Away', backgroundColor: "#79b821", icon:"st.Home.home3"        
        //attributeState "armedmax", label: 'Armed Instant Away', backgroundColor: "#79b821", icon:"st.Home.home3"
        //attributeState "alarmcleared", label: 'Alarm in Memory', backgroundColor: "#ffcc00", icon:"st.Home.home2"
        //attributeState "alarm", label: 'Alarm', backgroundColor: "#ff0000", icon:"st.Home.home3"
      }
      tileAttribute ("panelStatus", key: "SECONDARY_CONTROL") {
        attributeState "panelStatus", label:'${currentValue}'
      }
    }

    standardTile("armAwayButton","device.button", width: 2, height: 2, canChangeIcon: true, decoration: "flat") {
      state "default", label: 'Away', action: "armAway", icon: "st.Home.home3", backgroundColor: "#e74c3c"
    }
  
    standardTile("armInstantButton","device.button", width: 2, height: 2, canChangeIcon: true, decoration: "flat") {
      state "default", label: 'Instant', action: "armInstant", icon: "st.Home.home3", backgroundColor: "#a569bd"
    }
  /*
    standardTile("disarmButton","device.button", width: 2, height: 2, canChangeIcon: true, decoration: "flat") {
      state "default", label: 'Disarm', action: "disarm", icon: "st.Home.home2", backgroundColor: "#C0C0C0"
    }
 */     
    standardTile("refreshButton","device.button", width: 2, height: 2, canChangeIcon: true, decoration: "flat") {
      state "default", label: 'Refresh', action: "refreshStatus", icon: "st.secondary.refresh-icon", backgroundColor: "#2471a3"
    }
    main "partition"
    //main "showStatus"

    details(["partition",
             "armAwayButton",
             "armInstantButton",
             //"disarmButton",
             "refreshButton"])
  }
}

void setStatus(value){
	sendEvent (name: "dscpartition", value:value)
}

void setStatusUnknown(){
	setStatus("Unknown")
}

//use this when you can't just return a command
def invokeRefreshIn(seconds){
	runIn(seconds, invokeRefresh)
}

def invokeRefresh(){
    sendHubCommand(refreshStatus())
}
def refreshStatus(){
   log.debug "show status"
   setStatus("Refresh")
   return sendCommand("/GetSecurityStatus", "refreshStatusCalledBackHandler")
}
//void refreshStatusCalledBackHandler(physicalgraph.device.HubResponse hubResponse) {
void refreshStatusCalledBackHandler(response) {
    log.debug "Entered calledBackHandler..."
   // log.debug response
    
     def msg = parseLanMessage(response.description)
   // log.debug msg
   	def results = new groovy.json.JsonSlurper().parseText(msg.body)  
	log.debug("${results.Status}")

	if(results.Status.contains("SECS REMAINING")){
		setStatus("Arming")
        invokeRefreshIn(5)//refresh in 5 seconds
	}
   else{
   		setStatus("${results.Status}")
   }
}

def partition(String state, String alpha) {  
 log.debug "partition"
 // sendEvent (name: "dscpartition", value: "${state}", descriptionText: "${alpha}")
 // sendEvent (name: "panelStatus", value: "${alpha}", displayed: false)
}

def armAway() {
    log.debug "set away..."
	def path  = "/AdvancedSecurity/ArmWithCode?arming=AWAY&ucode=${userCode}&operation=set"
   return sendCommand(path, "armAwayCalledBackHandler");
}
void armAwayCalledBackHandler(response) {
    log.debug "Entered armAwayCalledBackHandler..."
   // log.debug response
    invokeRefreshIn(2)//refresh in 2 seconds
}

def armInstant() {
    log.debug "set Instant..."
	def path  = "/AdvancedSecurity/ArmWithCode?arming=NIGHT&ucode=${userCode}&operation=set"
    sendCommand(path, "armInstantCalledBackHandler");
}
void armInstantCalledBackHandler(response) {
    log.debug "Entered armInstantCalledBackHandler..."
   // log.debug response
   invokeRefreshIn(2)//refresh in 2 seconds
}
/*
def disarm() {
   //parent.sendCommand();
   sendCommand("/AdvancedSecurity/Disarm?ucode=${userCode}&operation=set");
 }
 */
 def sendCommand(String path, String callbackName, Boolean suppressWakeupCheck=false){
	log.debug "Building command..."
    def destIp = "192.168.1.164"
    def destPort = "80"
    def hosthex = convertIPtoHex(destIp)
    def porthex = convertPortToHex(destPort)
    //if you don't do this, it will not work!
    device.deviceNetworkId = "$hosthex:$porthex" 
	
    def params=[
                method: 'GET',
                path: '/system_http_api/API_REV01/' + path,
                headers: [ HOST: "$destIp:$destPort" ]
            ]
    def options=[
            	callback: callbackName
            ]
    def hubAction = new physicalgraph.device.HubAction(params, null, options) 
  //  log.debug hubAction
	if(suppressWakeupCheck){
    	log.debug "Wakeup check suppressed"
    	return hubAction
    }

	if(needsWakeUp()){
    	log.debug "Needs wakeup"
        state.afterWakeUpCommandParams = [path: path, callbackName: callbackName]
    	return buildWakeUpCommand(wakeupCallback)
    } else {
    	log.debug "No wakeup needed"
    	return hubAction
    }
}

def wakeupCallback(response){
	def dt=now()
    log.debug "processPendingActions and setting lastWakeUp to ${dt}"
	state.lastWakeUp=dt
    def params=state.afterWakeUpCommandParams
    log.debug "params are: $params"
    def cmd = sendCommand(params.path, params.callbackName, true)
	sendHubCommand(cmd)
    
    runIn(18000/60, setStatusUnknown)
}

def needsWakeUp(){
	if(state.lastWakeUp){
    	def msSinceLastWake=now()-state.lastWakeUp
    	//if it's been 5 mins, wake it up
    	return (msSinceLastWake>18000)        	 
    }
    log.debug "lastWakeUp not set on state" 
	return true
}

def buildWakeUpCommand( callback){
	log.debug "building wakeup command..."
    def destIp = "192.168.1.164"
    def destPort = "80"
    def hosthex = convertIPtoHex(destIp)
    def porthex = convertPortToHex(destPort)
    //if you don't do this, it will not work!
    device.deviceNetworkId = "$hosthex:$porthex" 
	
    def params=[
                method: 'GET',
                path: '/',
                headers: [ HOST: "$destIp:$destPort" ]
            ]
    def options=[
            	callback: callback
            ]
    def hubAction = new physicalgraph.device.HubAction(params, null, options) 
    //log.debug hubAction

    return hubAction
}

def parse(description) {
    log.debug "WARNING: parse starting, you should have provided a callback, else no way to know what you are parsing for!"
    log.debug description
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02X', it.toInteger() ) }.join()
    return hex
}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04X', port.toInteger() )
    return hexport
}