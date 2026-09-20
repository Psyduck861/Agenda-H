package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "partners")
data class Partner(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val location: String = "",
    val origin: String = "", // e.g., Tinder, Instagram, Casual, Safe
    val age: Int = 0,
    val affinity: Int = 50, // 0 to 100
    val photoUrl: String = "", // URL or empty
    val isFavorite: Boolean = false,
    val notes: String = "",
    val firstDate: String = "", // custom date string e.g., 12/03/25
    val rating: Double = 5.0, // 0 to 10
    
    // Spec additions for Woman
    val children: Int = 0, // filhos dela
    val myKids: Int = 0, // meus filhos com ela
    val objective: String = "Casual", // Relacionamento sério, Casual, Ver no que dá, Amizade, Vende packs, Incompatível
    val phone: String = "", // WhatsApp
    val address: String = "", // Cidade/endereço
    val availability: String = "Solteira", // Solteira, Casada, Indisponível, Mora longe, Perdeu contato
    val status: String = "Ativa", // Ativa / Inativa
    val negatives: String = "", // comma-separated traços negativos/desagradáveis
    
    // GP additions
    val isGP: Boolean = false,
    val gpPrice: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

fun Partner.getCity(): String {
    val isGpVal = this.isGP ?: false
    if (isGpVal) {
        val addr = (this.address ?: "").trim()
        if (addr.isEmpty()) return (this.location ?: "").trim().ifEmpty { "Sem local" }
        val lastComma = addr.lastIndexOf(',')
        return if (lastComma != -1) addr.substring(lastComma + 1).trim() else addr
    } else {
        val addr = (this.address ?: "").trim()
        val legacyLoc = (this.location ?: "").trim()
        if (addr.isEmpty()) {
            if (legacyLoc.isEmpty() || legacyLoc.contains("cidade") || legacyLoc.contains("muni") || legacyLoc.contains("próximo") || legacyLoc.contains("estado") || legacyLoc.contains("Exterior")) {
                return "Sem cidade"
            }
            return legacyLoc
        }
        val lastComma = addr.lastIndexOf(',')
        return if (lastComma != -1) {
            addr.substring(lastComma + 1).trim()
        } else {
            addr
        }
    }
}
