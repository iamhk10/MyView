package com.x.myview.domain

/** Short lines shown on the Today screen. One per day, or tap for the next. */
val QUOTES = listOf(
    "Small steps every day add up to big changes.",
    "You do not have to be extreme, just consistent.",
    "Don't break the chain.",
    "A little progress each day adds up to big results.",
    "Discipline is choosing what you want most over what you want now.",
    "Your future self is watching. Make them proud.",
    "Motivation gets you started. Habit keeps you going.",
    "Drink water. Move a little. Breathe. Repeat.",
    "The best workout is the one you actually do.",
    "Rest is part of the plan, not a break from it.",
    "Done is better than perfect.",
    "Today is a good day to start again.",
    "Strong habits build a strong life.",
    "Show up even when you do not feel like it.",
    "What you do daily matters more than what you do once.",
    "Be proud of every rep, every glass, every step.",
    "Energy follows action.",
    "You are one habit away from a better day.",
    "Progress, not perfection.",
    "Fall seven times, stand up eight.",
    "Your only competition is who you were yesterday.",
    "Calm mind, strong body.",
    "Make it easy, make it a habit, make it yours.",
    "Win the morning, win the day.",
    "Sleep well. Everything works better after it.",
    "Count your steps, then count your wins.",
    "Tiny habits, giant results.",
    "It always seems impossible until it is done.",
    "Take care of your body. It is the only place you have to live.",
    "Start where you are. Use what you have. Do what you can.",
)

fun quoteFor(day: Long, shift: Int = 0): String =
    QUOTES[Math.floorMod(day + shift, QUOTES.size.toLong()).toInt()]
