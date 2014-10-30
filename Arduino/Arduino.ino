// arduino.ino

#include <stdio.h>
#include <adk.h>
#include "DualMC33926MotorShield.h"
#define BUFFSIZE   128
#define MAX_POWER  400
// Accessory descriptor. It's how Arduino identifies itself in Android.
char accessoryName[] = "UDOO Mobile Tank";
char manufacturer[] = "Simone Mariotti";
char model[] = "Mobile-Tanker";

char versionNumber[] = "0.1.0";
char serialNumber[] = "1";
char url[] = "https://github.com/DMIAlumni/mobile-tanker";

// ADK configuration
USBHost Usb;
ADK adk(&Usb, manufacturer, model, accessoryName, versionNumber, url, serialNumber);
uint8_t buffer[BUFFSIZE];
uint32_t bytesRead = 0;
DualMC33926MotorShield tank;
void setup() {
  Serial.begin(115200);
  delay(1000);
  Serial.println("All power to the engines!");
  tank.init();
}

void loop() {
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
      Serial.println("Comando");
      //eepCommandInterpreter(extractMovement(), extractSpeed());
      Serial.println(buffer[0]);
    }
  }
}
