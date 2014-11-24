// arduino.ino

#include <stdio.h>
#include <adk.h>
#include "DualMC33926MotorShield.h"
#define BUFFSIZE   255
#define MAX_POWER  400
#define DELAY 50
// Android --> Arduino codes
#define CMD_NULL_VALUE 0
#define CMD_MOVE_FORWARD 1
#define CMD_MOVE_BACKWARD 6
#define CMD_STOP 2
#define CMD_LEFT 3
#define CMD_RIGHT 4
#define CMD_SHOOT 5
#define CMD_SEARCH 10

// Arduino --> Aandroid codes
// Message types
#define INFO = 0;
#define STATE = 1;
// States
#define IDLE = 100;
#define SEARCHING = 101;
#define HUNTING = 102;
#define EMERGENCY = 103;
// Actions
#define STOPPED = 150;
#define MOVING = 151;
// Infos
#define SHOOTED = 201;
#define RELOADED = 202;
#define DISTANCE = 203;

// Motors definition
//Ch A = LEFT, Ch B = RIGTH
const int
PWM_LEFT   = 3,
DIR_LEFT   = 12,
BRAKE_LEFT = 9,
SNS_LEFT  = A0;
const int
PWM_RIGHT   = 11,
DIR_RIGHT   = 13,
BRAKE_RIGHT = 8,
SNS_RIGHT  = A1;

int
command,
param1,
param2;



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
// Debug mode for communication
bool COM_DEBUG_MODE = false;
// Debug mode for movment
bool MOV_DEBUG_MODE = true;
uint8_t inBuffer[BUFFSIZE];
uint8_t outBuffer[BUFFSIZE];
char inStringBuffer[BUFFSIZE];
char outStringBuffer[BUFFSIZE];
uint32_t bytesRead = 0;


// Test Variabiles
int randomint;
int last_send = 0;

void setup() {
  Serial.begin(115200);
  delay(1000);
  Serial.println("All power to the engines!");

}

void loop() {
  if (COM_DEBUG_MODE) {
    Serial.print(".");
  }
  Usb.Task();
  // Check that ADK is available
  if (adk.isReady()) {
    char*a;
    if (strlen(a = readFromADK()) > 0) {
      if (decodeCommand(a, &command, &param1, &param2)){
      Serial.println("Fuori");
      Serial.println(command);
      Serial.println(param1);
      Serial.println(param2);
      }else{
      Serial.print("Command ");Serial.print(command);Serial.print(", param1 ");Serial.print(param1);Serial.print(", param2 ");Serial.print(param2);Serial.println(", is not valid!");
      }
    } else {
      Serial.print("*");
    }
    sendToADK( random(0, 2), random(0, 4) + random(1, 3) * 100, random(150, 152));
  }
  delay(DELAY);
}

char* readFromADK() {
  adk.read(&bytesRead, BUFFSIZE, inBuffer);
  memset(inStringBuffer, 0, BUFFSIZE);
  if (bytesRead > 0) {
    memcpy(inStringBuffer, inBuffer, bytesRead);
    if (COM_DEBUG_MODE) {
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
  if (COM_DEBUG_MODE) {
    Serial.print("\nSending | ");
    Serial.print(outStringBuffer);
    Serial.print(" | ");
    Serial.print(strlen(outStringBuffer));
    Serial.println(" bytes outgoing");
  }
  delay(DELAY / 10);
}

void sendToADKDelayed(int command, int param1, int param2) {
  int time = millis();
  if (time - last_send > 2000) {
    last_send = time;
    randomint = random(-1500, 1500);
    memset(outStringBuffer, 0, BUFFSIZE);
    sprintf(outStringBuffer, "%d,%d,%d", command, param1, param2);
    memcpy(outBuffer, outStringBuffer, BUFFSIZE);
    adk.write(strlen(outStringBuffer), outBuffer);
    if (COM_DEBUG_MODE) {
      Serial.print("\nSending | ");
      Serial.print(outStringBuffer);
      Serial.print(" | ");
      Serial.print(strlen(outStringBuffer));
      Serial.println(" bytes outgoing");
    }
  }
  delay(DELAY / 10);
}

bool decodeCommand(char* incoming, int* command, int* param1, int* param2) {
  // Incoming message pattern command,param1,param2
  char delimiter = ',';
  *command = atoi(strtok(incoming, &delimiter));
  *param1 = atoi(strtok(NULL, &delimiter));
  *param2 = atoi(strtok(NULL, &delimiter));
  //checking validity of decoded commanda
  if (*command == CMD_MOVE_FORWARD && (*param1 == CMD_NULL_VALUE || *param2 == CMD_NULL_VALUE)) {
    return false;
  }
  if (*command == CMD_MOVE_BACKWARD && (*param1 == CMD_NULL_VALUE || *param2 == CMD_NULL_VALUE)) {
    return false;
  }
  if (*command == CMD_STOP  && *param2 != CMD_NULL_VALUE) {
    return false;
  }
  if (*command == CMD_LEFT  && *param2 != CMD_NULL_VALUE) {
    return false;
  }
  if (*command == CMD_RIGHT && *param2 != CMD_NULL_VALUE) {
    return false;
  }
  if (COM_DEBUG_MODE || MOV_DEBUG_MODE) {
    Serial.println(*command);
    Serial.println(*param1);
    Serial.println(*param2);
  }
  return true;


}
void stopEngine() {

}

