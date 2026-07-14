package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deliveries")
data class Delivery(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long, // timestamp (milisegundos)
    val time: String, // "14:30"
    val establishmentId: Int, // ID do estabelecimento cadastrado
    val establishmentName: String,
    val clientName: String? = null,
    val neighborhood: String,
    val city: String,
    val value: Double,
    val paymentMethod: String, // "Pix", "Dinheiro", "Cartão", "Outro"
    val distanceKm: Double,
    val deliveryTimeMinutes: Int? = null,
    val notes: String = "",
    val quantity: Int = 1,
    val feePerDelivery: Double = 0.0,
    val feeFartherDeliveries: Double = 0.0
)

@Entity(tableName = "establishments")
data class Establishment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val address: String = "",
    val neighborhood: String = "",
    val city: String = "",
    val phone: String = "",
    val contact: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "fuel_logs")
data class FuelLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long,
    val gasStation: String,
    val totalAmount: Double,
    val liters: Double,
    val odometer: Double
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long,
    val category: String, // e.g. "Combustível", "Troca de óleo", "Filtro de óleo", "Outros"
    val value: Double,
    val notes: String = ""
)

@Entity(tableName = "maintenances")
data class Maintenance(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val item: String, // "Troca de óleo", "Filtro", "Vela", "Corrente", etc.
    val date: Long,
    val currentOdometer: Double,
    val cost: Double,
    val nextOdometer: Double,
    val notes: String = ""
)

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "Diária", "Semanal", "Mensal", "Anual"
    val targetValue: Double
)
