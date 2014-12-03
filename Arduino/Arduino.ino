// arduino.ino

#include <stdio.h>
#include <adk.h>
#define LED_GREEN 53
#define LED_YELLOW 52
#define LED_RED 7
#define TERMINATE_BUTTON 51
#define BUFFSIZE   255
#define MAX_POWER  400
#define DELAY 50 // Smaller delay breaks everything about communication

// Android --> Arduino codes
#define CMD_NULL_VALUE 0
#define CMD_MOVE_FORWARD 1
#define CMD_MOVE_BACKWARD 6
#define CMD_STOP 2
#define CMD_LEFT 3
#define CMD_RIGHT 4
#define CMD_SHOOT 5
#define CMD_SEARCH 10

#define TURN_ON_SPOT 1
#define TURN_NORMALLY 2

// Arduino --> Aandroid codes
// Message types
#define INFO 0
#define STATE 1
// States
#define IDLE 100
#define SEARCHING 101
#define HUNTING 102
#define EMERGENCY 103
// Actions
#define STOPPED 150
#define MOVING 151
// Infos
#define SHOOTED 201
#define RELOADED 202
#define DISTANCE 203
#define TERMINATE 999

//Ultrasonic Ranging Module
#define TRIG_PIN 2
#define ECHO_PIN 4

//Emergency Sensors
#define FL A5
#define FR A4


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
//Ch A = LEFT, Ch B = RIGHT
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
baseVelocity = 131,
velocityStep= 5;

long coldTimerStart;





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
bool COM_DEBUG_MODE = true;
// Debug mode for movment
bool MOV_DEBUG_MODE = false;
uint8_t inBuffer[BUFFSIZE];
uint8_t outBuffer[BUFFSIZE];
char inStringBuffer[BUFFSIZE];
char outStringBuffer[BUFFSIZE];
uint32_t bytesRead = 0;
int blinkGreenTimer, blinkRedTimer;
bool stateRed,stateGreen;
bool emergency_mode,warning;
int emergency_sensor_threshold =900;


// Test Variabiles
int randomint;
int last_send = 0;


void setup() {
  // Configure the A output
  pinMode(BRAKE_LEFT, OUTPUT);  // Brake pin on channel A
  pinMode(DIR_LEFT, OUTPUT);    // Direction pin on channel A
  pinMode(BRAKE_RIGHT, OUTPUT);  // Brake pin on channel B
  pinMode(DIR_RIGHT, OUTPUT);    // Direction pin on channel B
  pinMode(LED_GREEN, OUTPUT);
  pinMode(LED_YELLOW, OUTPUT);
  pinMode(LED_RED, OUTPUT);
  pinMode(TRIG_PIN, OUTPUT);
  pinMode(ECHO_PIN, INPUT);
  pinMode(TERMINATE_BUTTON, INPUT);
  //pinMode(FL, INPUT);
  Serial.begin(115200);
  delay(1000);
  Serial.println("All power to the engines!");
  stop(SOFT);
  blinkGreenTimer=blinkRedTimer= millis();
  stateGreen=stateRed=true;
  emergency_mode=warning=false;
  digitalWrite(LED_GREEN, LOW);
  digitalWrite(LED_YELLOW, LOW);
  digitalWrite(LED_RED, LOW);
}

void loop() {
  if (digitalRead(TERMINATE_BUTTON)==HIGH){    
    terminateAndroidApp();
    stop(HARD);
    Serial.println("Android App Closed. Push reset button to restart all.");
    while(1);
  }
  //Go in emergency mode after two bad readings from sensor
  if (MOV_DEBUG_MODE){
    Serial.print("FL reading: ");
    Serial.println(analogRead(FL));
  }
  if (!sensorsOk() && warning){
    emergency_mode=true;    
    emergency();
  }else if (!sensorsOk() && !warning){
    warning=true;
    Serial.println("Emergency warning");
  } 
  else {
    warning=false;
  }
  if (COM_DEBUG_MODE) {
    Serial.print(".");
  }


  Usb.Task();
  // Check that ADK is available
  if (adk.isReady()) {
    char*a;
    if (strlen(a = readFromADK()) > 0) {
      if (decodeCommand(a, &command, &param1, &param2)) {
        if (MOV_DEBUG_MODE && (command == CMD_LEFT || command== CMD_RIGHT)){
          Serial.print("COMMAND: Command "); Serial.print(command); Serial.print(", param1 "); Serial.print(param1); Serial.print(", param2 "); Serial.print(param2); Serial.println(", received and is beign processed!");
        }
        switch (command) {
          case CMD_LEFT:
          turnLeft(param1, param2);
          break;
          case CMD_RIGHT:
          turnRight(param1, param2);
          break;
          case CMD_STOP:
          stop(HARD);
          break;
          case CMD_MOVE_FORWARD:
          moveForward(param1,param2);
          break;
          default:
          stop(HARD);
          if (COM_DEBUG_MODE) {
            Serial.println("Default Block");
          }
          break;
        }
        lastCommand=command;
      } 
      else {
        if (MOV_DEBUG_MODE){
          Serial.print("ERROR: Command "); Serial.print(command); Serial.print(", param1 "); Serial.print(param1); Serial.print(", param2 "); Serial.print(param2); Serial.println(", is not valid!");
        }
      }
      if (millis()-blinkGreenTimer>300){
        stateGreen=!stateGreen;
        blinkGreenTimer=millis();
        digitalWrite(LED_GREEN, stateGreen);
      }
    } 
    else {
      if (millis()-blinkRedTimer>300){
        stateRed=!stateRed;
        blinkRedTimer=millis();
        digitalWrite(LED_YELLOW, stateRed);   
      }
      Serial.print("*");
    }
    getDistance();
    //sendToADK( random(0, 2), random(0, 4) + random(1, 3) * 100, random(150, 152));
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
  if (*command == CMD_LEFT  && (*param1 == CMD_NULL_VALUE || *param2 == CMD_NULL_VALUE)) {
    return false;
  }
  if (*command == CMD_RIGHT && (*param1 == CMD_NULL_VALUE || *param2 == CMD_NULL_VALUE)) {
    return false;
  }
  if (COM_DEBUG_MODE || MOV_DEBUG_MODE) {
    Serial.println(*command);
    Serial.println(*param1);
    Serial.println(*param2);
  }
  return true;

}
void getDistance(){
  long duration, distance;
  digitalWrite(TRIG_PIN, LOW); 
  delayMicroseconds(2); 
  digitalWrite(TRIG_PIN, HIGH);
  delayMicroseconds(10);
  digitalWrite(TRIG_PIN, LOW);
  //The module return an high signal with duration equal to the sound travel time
  duration = pulseIn(ECHO_PIN, HIGH, 2000);//ignores target far more than 71cm
  // Divide half 
  distance = (duration/2) / 29.1; 
  if (COM_DEBUG_MODE) { 
    Serial.print(distance);Serial.println("cm");
  }
  sendToADK(INFO,DISTANCE,(int)distance);
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
void moveForward(int velocityLeft,int velocityRight){
  setDirection(BOTH,FORWARD);
  releaseBrake(BOTH);
  analogWrite(PWM_LEFT, velocityLeft);
  analogWrite(PWM_RIGHT, velocityRight);
}

void moveBackward(int velocityLeft,int velocityRight){
  setDirection(BOTH,BACKWARD);
  releaseBrake(BOTH);
  analogWrite(PWM_LEFT, velocityLeft);
  analogWrite(PWM_RIGHT, velocityRight);
}

void turnLeft(int velocity, int mode){
  if (lastCommand!=CMD_LEFT){
    stop(HARD); 
  }
  setDirection(RIGHT, FORWARD);    
  releaseBrake(RIGHT);
  analogWrite(PWM_RIGHT, velocity);
  if (mode==TURN_ON_SPOT){
    setDirection(LEFT, BACKWARD);
    releaseBrake(LEFT);
    analogWrite(PWM_LEFT, velocity);
  }
}

void turnRight(int velocity, int mode){
  if (lastCommand!=CMD_RIGHT){
   stop(HARD);
 }
 setDirection(LEFT, FORWARD);
 releaseBrake(LEFT);
 analogWrite(PWM_LEFT, velocity);
 if (mode==TURN_ON_SPOT){
  setDirection(RIGHT, BACKWARD);
  releaseBrake(RIGHT);
  analogWrite(PWM_RIGHT, velocity);
}
}

// with method = HARD = true it uses brakes, otherwise stop by inertia 
void stop(bool method) {
  if (method){ 
    brake(BOTH);
    analogWrite(PWM_LEFT, LOW);
    analogWrite(PWM_RIGHT, LOW);
  } 
  else{
    analogWrite(PWM_LEFT, LOW);
    analogWrite(PWM_RIGHT, LOW);
  }
  delay(DELAY / 10);
}

void emergency(){
  Serial.println("****EMERGENCY***");
  digitalWrite(LED_GREEN, LOW);
  digitalWrite(LED_YELLOW, LOW);
  digitalWrite(LED_RED, HIGH);
  // rescue action
  stop(HARD);
  moveBackward(170,140);
  delay(1000);
  stop(SOFT);
    //TODO if is not all ok, call it recursively
    emergency_mode=warning=false;
  //
  Serial.println("EMERGENCY ENDED");
  digitalWrite(LED_RED, LOW);
}

bool sensorsOk(){
  return analogRead(FL)<emergency_sensor_threshold && analogRead(FR)<emergency_sensor_threshold;
}

void terminateAndroidApp(){
  sendToADK(INFO,TERMINATE,CMD_NULL_VALUE);  
}

void setSpeedAndGo(int velocityLeft, int velocityRight) {
  releaseBrake(BOTH); 

  if (millis()-coldTimerStart>100 ) {
    if ( currentLeftVelocity<velocityLeft+10 && currentLeftVelocity!=LOW){
      currentLeftVelocity= currentLeftVelocity+velocityStep/5;
    }
    if (currentRightVelocity<velocityRight){


      currentRightVelocity= currentRightVelocity+velocityStep/5;

    }
    analogWrite(PWM_LEFT, currentLeftVelocity);
    analogWrite(PWM_RIGHT, currentRightVelocity); 
    Serial.println("");
    Serial.print("Current velocity SX: ");Serial.println(currentLeftVelocity);
    Serial.print("Current velocity DX: ");Serial.println(currentRightVelocity);
    Serial.print(analogRead(SNS_LEFT));Serial.print("HHHH");Serial.println(analogRead(SNS_RIGHT));
    coldTimerStart=millis();

  }
}


void turnLeft2(int velocity){
  if (lastCommand!=CMD_LEFT){
    stop(HARD); 
    coldTimerStart=millis(); 
    currentLeftVelocity=LOW;
    currentRightVelocity=baseVelocity;
  }
  setDirection(RIGHT, FORWARD);
  setDirection(LEFT, BACKWARD);
  setSpeedAndGo(LOW,velocity);
}

void turnRight2(int velocity){
  if (lastCommand!=CMD_RIGHT){
   stop(HARD); 
   coldTimerStart=millis();
   currentRightVelocity=LOW;
   currentLeftVelocity=baseVelocity;
 }
 setDirection(RIGHT, BACKWARD);
 setDirection(LEFT, FORWARD);
 setSpeedAndGo(velocity,LOW);
}

// with method = HARD = true it uses brakes, otherwise stop by inertia 
void stop2(bool method) {
  if (method){ 
    brake(BOTH);
    currentRightVelocity=currentLeftVelocity=LOW;
    analogWrite(PWM_LEFT, LOW);
    analogWrite(PWM_RIGHT, LOW);
    } else{
      currentRightVelocity=currentLeftVelocity=LOW;
      setSpeedAndGo(LOW,LOW);
    }
    delay(DELAY / 10);
  }
