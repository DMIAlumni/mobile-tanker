// arduino.ino

#include <stdio.h>
#include <adk.h>
#include "DualMC33926MotorShield.h"
#define BUFFSIZE   128
#define MAX_POWER  400

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

int randomint;
int last_send = 0;
uint8_t buffer[BUFFSIZE];
char bufNew[BUFFSIZE];
char charBuf[2000];
uint32_t bytesRead = 0;

DualMC33926MotorShield tank;

void setup() {
  Serial.begin(115200);
  delay(1000);
  Serial.println("All power to the engines!");
  tank.init();
}

void loop() {
  Serial.println("Looping");
  readingFromADK();
}

void stopEngine() {
  tank.setM1Speed(0);
  tank.setM2Speed(0);
}

void readingFromADK() {
  Usb.Task();
  // Starting listening when ADK is available
  if (adk.isReady()) {
    adk.read(&bytesRead, BUFFSIZE, buffer);
    if (bytesRead > 0) {
      for (int i = 0; i < bytesRead; i++) {
        if (buffer[i] >= 48 && buffer[i] <= 47)
          Serial.print(buffer[i] - 48);
        else
          Serial.print((char)buffer[i]);
        Serial.print("-");
        //eepCommandInterpreter(extractMovement(), extractSpeed());
        //eepCommandInterpreter(extractMovement(), extractSpeed());
      }	Serial.print("RECEIVED FROM USB: ");
      Serial.println(*(uint32_t*)buffer);
    }
  } sendingToADK();
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
  }
  writeToAdk(charBuf);
}

void writeToAdk(char textToSend[]) {
  Serial.print(textToSend);
  Serial.print(" ");
  Serial.print(strlen(textToSend));
  Serial.println(" bytes in partenza");
  adk.write(strlen(textToSend), (uint8_t*)textToSend);
}
