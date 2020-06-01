//***   Biblioteki użyte podczas wykonywania projektu   ***
#include <OneWire.h>
#include <DallasTemperature.h>
#include <LiquidCrystal.h> // bibliotek ekranu
#include <Keypad.h>
#include <DHT.h>
//#include <Adafruit_Sensor.h>
//#include <Arduino.h>
//***   Przypisane piny do odpowiednich częsci   ***
LiquidCrystal lcd(3, 2, 4, 5, 6, 7); // piny wyświetlacza
 
#define ONE_WIRE_BUS 8 // pin czujnika : Dallas
#define DHTPIN 9     // pin czujnika : DHT
#define DHTTYPE DHT22   
DHT dht(DHTPIN, DHTTYPE);
//*** Zdefiniowanie użytych czujników   ***
OneWire oneWire(ONE_WIRE_BUS);
DallasTemperature sensors(&oneWire);
DeviceAddress Czujnik_1, Czujnik_2; 

//***   Zmienne poszczególnych temperatur   ***
float Celsiusz = 0;
float Fahrenheit = 0;

//***   Ustawienia klawiatury   ***
const byte ROWS = 4;
const byte COLS = 4;
char keys[ROWS][COLS] = {
  {'1','2','3','A'},
  {'4','5','6','B'},
  {'7','8','9','C'},
  {'*','0','#','D'}};
byte rowPins[ROWS] = {20,22,24,26}; //piny klawiatury
byte colPins[COLS]= {28,30,32,34};  //piny klawiatury
Keypad keypad = Keypad( makeKeymap(keys), rowPins, colPins, ROWS, COLS );

// inicjacja potrzebnych ustawień w celu poprawnego uruchomienia systemu
void setup()
{
dht.begin();
pinMode(ONE_WIRE_BUS,INPUT_PULLUP);
lcd.begin(16,2); 
Serial.begin(9600);
sensors.begin();
sensors.getAddress(Czujnik_1, 0); 
sensors.getAddress(Czujnik_2, 1); 

}


// Funkcja identyfikująca adres kazdego z czujników typu Dallas
void printAddress(DeviceAddress deviceAddress)
{
  for (uint8_t i = 0; i < 8; i++)
  {
    if (deviceAddress[i] < 16) lcd.print("");
    Serial.print(deviceAddress[i], HEX);
  }

}
// funkcja pobrania temperatury z czujników na podstawie ich adresu
float getTemperature(DeviceAddress deviceAddress)
{
  return sensors.getTempC(deviceAddress);


}


// funkcja wypisania odpowiednich informacji odczytanych z czujnika
void printData(DeviceAddress deviceAddress)
{
 // printAddress(deviceAddress);
  printTemperature(deviceAddress);


}
// funkcja wyświetlająca temperature
void printTemperature(DeviceAddress deviceAddress)
{
  float tempC = sensors.getTempC(deviceAddress);
  lcd.print(tempC);
  lcd.print(" C");
}



void loop()
{


 // 
  // zmienne odpowiedzialne za operacje porównania czujnika idealnego oraz kalibrowanego
 float x = getTemperature(Czujnik_1);
  float y =  getTemperature(Czujnik_2);
  float z = y - x;

 
char klawisz = keypad.getKey(); 
// Instrukcje wykonywana po przycisnieciu odpowiednich przycisków
// Funckja przycisku '5'
if(klawisz =='5')
{
lcd.print("Wybrano:");
lcd.setCursor(2,1);
lcd.print("Dallas");
delay(2000);
lcd.clear();

  for (int x=0; x<20; x++)
  {
sensors.requestTemperatures();
Celsiusz = sensors.getTempCByIndex(0);
Fahrenheit = sensors.toFahrenheit(Celsiusz);
lcd.print(Celsiusz);
lcd.print(" C  ");    
lcd.setCursor(0,1); 
lcd.print(Fahrenheit);
lcd.print(" F");
delay(2000);
lcd.clear();
  } 
  
}
// Funckja przycisku '*'
if (klawisz == '*')
{
lcd.print("Wybrano:");
lcd.setCursor(2,1);
lcd.print("DHT");
delay(2000);
lcd.clear();
    for (int x=0; x<20; x++)
  {
float Celsiusz = dht.readTemperature();
Fahrenheit = 1.8 *Celsiusz+32;
lcd.print(Celsiusz);
lcd.print(" C  ");
//delay(1000);    
lcd.setCursor(0,1); 
lcd.print(Fahrenheit);
lcd.print(" F");
delay(2000);
lcd.clear();
  }
}
// Funckja przycisku '6'
if (klawisz == '6')
{
lcd.print("Wybrano:");
lcd.setCursor(2,1);
lcd.print("Porownanie");
delay(3500);
lcd.clear();
lcd.print("T1:");
printData(Czujnik_1);
lcd.setCursor(0,1); 
lcd.print("T2:");
printData(Czujnik_2);
delay(5000);
lcd.clear();
lcd.setCursor(0,0);
lcd.print("Roznica wynosi:");
delay(2000);
lcd.setCursor(2,16);
lcd.print(z);
lcd.print(" C");
 Serial.print("Device 0 Address: ");
  printAddress(Czujnik_1);
  Serial.println();

  Serial.print("Device 1 Address: ");
  printAddress(Czujnik_2);
  Serial.println();
  }
}
