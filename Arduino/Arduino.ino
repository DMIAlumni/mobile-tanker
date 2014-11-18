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

uint8_t inBuffer[BUFFSIZE];
uint8_t outBuffer[BUFFSIZE];
char inStringBuffer[BUFFSIZE];
char outStringBuffer[BUFFSIZE];
uint32_t bytesRead = 0;

DualMC33926MotorShield tank;

// Test Variabiles
int randomint;
int last_send = 0;

void setup() {
  Serial.begin(115200);
  delay(1000);
  Serial.println("All power to the engines!");
  tank.init();
}

void loop() {
  if (DEBUG_MODE) {
    Serial.print(".");
  }
  Usb.Task();
  // Check that ADK is available
  if (adk.isReady()) {
    char*a;
    if (strlen(a = readFromADK()) > 0) {
      Serial.println(strlen(a));
    } else {
      Serial.print("*");
    }
    sendToADK(millis(), millis(), millis());
  }
  delay(DELAY);
}

char* readFromADK() {
  adk.read(&bytesRead, BUFFSIZE, inBuffer);
  memset(inStringBuffer, 0, BUFFSIZE);
  if (bytesRead > 0) {
    memcpy(inStringBuffer, inBuffer, bytesRead);
    if (DEBUG_MODE) {
      Serial.print("\nReceiving | ");
      Serial.print(inStringBuffer);
      Serial.print(" | ");
      Serial.print(strlen(inStringBuffer));
      Serial.println(" bytes incoming");
    }
  }
  delay(DELAY / 10);
  return inStringBuffer; // If nothing has been read then return the previously initialized empty array
}

void sendToADK(int command, int param1, int param2) {
  memset(outStringBuffer, 0, BUFFSIZE);
  sprintf(outStringBuffer, "%d,%d,%d", command, param1, param2);
  memcpy(outBuffer, outStringBuffer, BUFFSIZE);
  adk.write(strlen(outStringBuffer), outBuffer);
  if (DEBUG_MODE) {
    Serial.print("\nSending | ");
    Serial.print(outStringBuffer);
    Serial.print(" | ");
    Serial.print(strlen(outStringBuffer));
    Serial.println(" bytes outgoing");
  }
  delay(DELAY / 10);
}

void sendToADKDelayed() {
  int time = millis();
  if (time - last_send > 5000) {
    last_send = time;
    randomint = random(-1500, 1500);
    memset(outStringBuffer, 0, BUFFSIZE);
    sprintf(outStringBuffer, "%d,%d,%d", randomint, randomint - 1000, randomint + 1000);
    memcpy(outBuffer, outStringBuffer, BUFFSIZE);
    adk.write(strlen(outStringBuffer), outBuffer);
    if (DEBUG_MODE) {
      Serial.print("\nSending | ");
      Serial.print(outStringBuffer);
      Serial.print(" | ");
      Serial.print(strlen(outStringBuffer));
      Serial.println(" bytes outgoing");
    }
  }
  delay(DELAY / 10);
}


void stopEngine() {
  tank.setM1Speed(0);
  tank.setM2Speed(0);
}

