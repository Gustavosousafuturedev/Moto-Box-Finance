package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "motorcycles")
@Serializable
data class Motorcycle(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val brand: String,
    val model: String,
    val year: Int,
    val plate: String,
    val color: String,
    val currentOdometer: Double,
    val oilChangeInterval: Double = 3000.0
)

@Entity(tableName = "maintenances")
@Serializable
data class Maintenance(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val motorcycleId: Int,
    val type: String, // e.g. "Oil Change", "Brakes", "Tires", "Filters", "Other"
    val title: String,
    val odometer: Double,
    val cost: Double,
    val notes: String,
    val dateMillis: Long
)

@Entity(tableName = "refuels")
@Serializable
data class Refuel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val motorcycleId: Int,
    val odometer: Double,
    val liters: Double,
    val pricePerLiter: Double,
    val totalCost: Double,
    val dateMillis: Long
)

@Entity(tableName = "deliveries")
@Serializable
data class Delivery(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateMillis: Long = System.currentTimeMillis(),
    val establishment: String,
    val value: Double,
    val paymentMethod: String, // "PIX" or "Dinheiro"
    val notes: String = ""
)

@Entity(tableName = "expenses")
@Serializable
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateMillis: Long = System.currentTimeMillis(),
    val title: String,
    val cost: Double,
    val category: String, // "Alimentação", "Pedágio", "Estacionamento", "Outros"
    val notes: String = ""
)

@Entity(tableName = "user_profiles")
@Serializable
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val phone: String = "",
    val city: String = "",
    val motorcycleBrand: String = "",
    val motorcycleModel: String = "",
    val motorcycleYear: Int = 0,
    val motorcyclePlate: String = "",
    val currentOdometer: Double = 0.0,
    val photoUri: String = ""
)
