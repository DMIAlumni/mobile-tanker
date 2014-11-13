// arduino.ino

#include <stdio.h>
#include <adk.h>
#include "DualMC33926MotorShield.h"
#define BUFFSIZE   255
#define MAX_POWER  400
#define DELAY 50


// Start Accessory Descriptor. It's how Arduino identifies itself in Android.
char accessoryName[] = "UDOO Mobile Tank";
char manufacturer[] = "Simone Mariotti";
char model[] = "Mobile-Tanker";
char versionNumber[] = "0.1.0";
char serialNumber[] = "1";
char url[] = "https://github.com/DMIAlumni/mobile-tanker";
// End Accessory Descriptor

// Start ADK configuration
USBHost Usb;
ADK adk(&Usb, manufacturer, model, accessoryName, versionNumber, url, serialNumber);
// End ADK configuration
bool DEBUG_MODE = true;
int randomint;
int last_send = 0;
uint8_t inBuffer[BUFFSIZE];
uint8_t outBuffer[BUFFSIZE];
char inStringBuffer[BUFFSIZE];
char outStringBuffer[BUFFSIZE];
uint32_t bytesRead = 0;

char charBuf[255];
char bufNew[255];
DualMC33926MotorShield tank;

void setup() {
  Serial.begin(115200);
  delay(1000);
  Serial.println("All power to the engines!");
  tank.init();
}

void loop() {
  Serial.print(".");
  Usb.Task();
  // Starting listening when ADK is available
  if (adk.isReady()) {
    readFromADK();
    sendingToADK();
  }

}

void stopEngine() {
  tank.setM1Speed(0);
  tank.setM2Speed(0);
}

char* readFromADK() {
  adk.read(&bytesRead, BUFFSIZE, inBuffer);
  if (bytesRead > 0) {
    memset(inStringBuffer, 0, BUFFSIZE);
    memcpy(inStringBuffer, inBuffer, bytesRead);
    if (DEBUG_MODE) {
      Serial.print("\nReceiving | ");
      Serial.print(inStringBuffer);
      Serial.print(" | ");
      Serial.print(strlen(inStringBuffer));
      Serial.println(" bytes incoming");      
    }
    delay(DELAY/10);
    return inStringBuffer;   
  }return {0}; // 0 is the command for NOP
}


void sendingToADK() {
  int time = millis();
  if (time - last_send > 5000) {
    last_send = time;
    randomint = random(-1500, 1500);
    String s = String("");
    s += randomint;
    s += ":";
    s += randomint - 1000;
    s += ":";
    s += randomint;
    s += ";";

    s.toCharArray(charBuf, sizeof(charBuf));
    //charBuf[0]='a';
    delay(10);
    for (int i = 0; i < s.length(); i++) {
      bufNew[i] = s.charAt(i);
    }
    memset(outStringBuffer, 0, BUFFSIZE);
    sprintf(outStringBuffer, "%d,%d,%d", randomint, randomint - 1000, randomint + 1000);    
    memcpy(outBuffer, outStringBuffer, BUFFSIZE);
    adk.write(strlen(outStringBuffer), (uint8_t*)outBuffer);
      if (DEBUG_MODE) {
    Serial.print("Sending | ");
    Serial.print(outStringBuffer);
    Serial.print(" | ");
    Serial.print(strlen(outStringBuffer));
    Serial.println(" bytes outgoing");
  }
  }
  

  //writeToAdk(charBuf);
  delay(DELAY);
}


