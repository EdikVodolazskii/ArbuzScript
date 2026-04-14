#include <stdio.h>
#include <stdbool.h>

int factorial(int n) {
    if (n <= 1) 
{
    return 1;
}
    return n * factorial(n - 1);
}

int num = 5;
int result = factorial(num);
int counter = 10;
while (counter > 0) 
{
    counter = counter - 1;
}
{
    int i = 0;
    while (i < 5) 
{
    {
    int temp = i * 2;
}
    i = i + 1;
}
}
if (result >= 100 && counter == 0) 
{
    bool success = true;
} else 
{
    bool success = false;
}
typedef struct {
    int hp;
    int power;
} Hero;

int Hero_takeDamage(Hero* this, int damage) {
    this->hp = this->hp - damage;
    return this->hp;
}


Hero warrior = (Hero){0};
warrior.hp = 100;
warrior.power = 50;
int remainingHp = Hero_takeDamage(&warrior, 20);
if (remainingHp < 50) 
{
    char* status = "Warning: Low HP";
}

