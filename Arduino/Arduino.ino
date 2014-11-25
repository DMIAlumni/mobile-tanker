// arduino.ino

#include <stdio.h>
#include <adk.h>
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
#define INFO 0;
#define STATE 1;
// States
#define IDLE 100;
#define SEARCHING 101;
#define HUNTING 102;
#define EMERGENCY 103;
// Actions
#define STOPPED 150;
#define MOVING 151;
// Infos
#define SHOOTED 201;
#define RELOADED 202;
#define DISTANCE 203;

const int
LEFT = 0,
RIGHT = 1,
BOTH = 2;

const bool
FORWARD = HIGH,
BACKWARD = LOW,
HARD = true,
SOFT = false;

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
param2,
lastCommand=CMD_STOP,
targetLeftVelocity = 0,
targetRightVelocity = 0,
currentLeftVelocity = 0,
currentRightVelocity= 0,
baseVelocity = 100,
velocityStep= 5;






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
  // Configure the A output
  pinMode(BRAKE_LEFT, OUTPUT);  // Brake pin on channel A
  pinMode(DIR_LEFT, OUTPUT);    // Direction pin on channel A
  pinMode(BRAKE_RIGHT, OUTPUT);  // Brake pin on channel B
  pinMode(DIR_RIGHT, OUTPUT);    // Direction pin on channel B

  Serial.begin(115200);
  delay(1000);
  Serial.println("All power to the engines!");
  stop(SOFT);

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
      if (decodeCommand(a, &command, &param1, &param2)) {
        Serial.print("COMMAND: Command "); Serial.print(command); Serial.print(", param1 "); Serial.print(param1); Serial.print(", param2 "); Serial.print(param2); Serial.println(", received and is beign processed!");
        switch (command) {
          case CMD_LEFT:
          turnLeft(param1);
          break;
          case CMD_RIGHT:
          turnRight(param1);
          break;
          case CMD_STOP:
          stop(SOFT);
          break;
          default:
          stop(SOFT);
          Serial.println("Default Block");
          break;
        }
        lastCommand=command;
        } else {
          Serial.print("ERROR: Command "); Serial.print(command); Serial.print(", param1 "); Serial.print(param1); Serial.print(", param2 "); Serial.print(param2); Serial.println(", is not valid!");
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
  if (*command == CMD_STOP  && (*param1 != CMD_NULL_VALUE || *param2 != CMD_NULL_VALUE)) {
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

void setDirection(int side, bool direction) {
  if (side == LEFT) {
    digitalWrite(DIR_LEFT, direction);
  }
  else if (side == RIGHT) {
    digitalWrite(DIR_RIGHT, !direction);
  } 
  else if (side == BOTH) {
    digitalWrite(DIR_RIGHT, !direction);
    digitalWrite(DIR_LEFT, direction);
  }
}

void brake(int side) {
  switch(side){
    case LEFT:
    digitalWrite(BRAKE_LEFT, HIGH);
    break;
    case RIGHT:
    digitalWrite(BRAKE_RIGHT, HIGH);
    break;
    case BOTH:
    digitalWrite(BRAKE_LEFT, HIGH);
    digitalWrite(BRAKE_RIGHT, HIGH);
    break;
    default:
    break;
  }
}

void releaseBrake(int side) {
  switch(side){
    case LEFT:
    digitalWrite(BRAKE_LEFT, LOW);
    break;
    case RIGHT:
    digitalWrite(BRAKE_RIGHT, LOW);
    break;
    case BOTH:
    digitalWrite(BRAKE_LEFT, LOW);
    digitalWrite(BRAKE_RIGHT, LOW);
    break;
    default:
    break;
  }
}
void setSpeedAndGo(int velocityLeft, int velocityRight) {
  targetLeftVelocity=velocityLeft;
  targetRightVelocity=velocityRight;
  if (currentLeftVelocity<targetLeftVelocity){
    currentLeftVelocity+=velocityStep;
  } 
  else if (currentLeftVelocity>targetLeftVelocity){
    currentLeftVelocity-=velocityStep;
  }
  if (currentRightVelocity<targetRightVelocity){
    currentRightVelocity+=velocityStep;
  } 
  else if (currentRightVelocity>targetRightVelocity){
    currentRightVelocity-=velocityStep;
  }
  releaseBrake(BOTH);  
  analogWrite(PWM_LEFT, currentLeftVelocity);
  analogWrite(PWM_RIGHT, currentRightVelocity); 
  delay(DELAY); 
}


void turnLeft(int velocity){
  if (lastCommand!=CMD_LEFT){
    stop(HARD);  
  }
  setDirection(RIGHT, FORWARD);
  setDirection(LEFT, BACKWARD);
  setSpeedAndGo(velocity,velocity);
}

void turnRight(int velocity){
  if (lastCommand!=CMD_RIGHT){
   stop(HARD); 
 }
 setDirection(RIGHT, BACKWARD);
 setDirection(LEFT, FORWARD);
 setSpeedAndGo(velocity,velocity);
}

// with method = HARD = true it uses brakes, otherwise stop by inertia 
void stop(bool method) {
  if (method){ 
    brake(BOTH);
    currentRightVelocity=currentLeftVelocity=LOW;
    analogWrite(PWM_LEFT, LOW);
    analogWrite(PWM_RIGHT, LOW);
    } else{
      setSpeedAndGo(LOW,LOW);
    }
    delay(DELAY / 10);
  }
