package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "encontros")
data class Encontro(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val partnerId: Int,
    val partnerName: String,
    val date: String, // e.g. "2026-05-21" or "21/05/2026"
    val time: String = "", // e.g. "21:00"
    val motelCost: Double = 0.0,
    val typeOral: Boolean = false,
    val typeAnal: Boolean = false,
    val typeVaginal: Boolean = false,
    val typeCreampie: Boolean = false,
    val typeFacial: Boolean = false,
    val typeSquirt: Boolean = false,
    val typeDeepthroat: Boolean = false,
    val typeFirstEncounter: Boolean = false,
    val typeVirginity: Boolean = false,
    val notes: String = "",
    val hadSex: Boolean = true, // se rolou sexo no encontro
    val isPregnancy: Boolean = false, // se houve registro de "vou ser papai"
    val rating: Double = 5.0, // nota do encontro (de 0 a 10)
    
    // Counter columns for fetishes (cumulative frequency/count)
    val countOral: Int = 0,
    val countAnal: Int = 0,
    val countVaginal: Int = 0,
    val countCreampie: Int = 0,
    val countFacial: Int = 0,
    val countSquirt: Int = 0,
    val countDeepthroat: Int = 0,
    val countAnalCreampie: Int = 0,

    // Campos para encontros comuns (Aba Mulheres)
    val isFirstEncontroSex: Boolean = false,
    val isVirginityLost: Boolean = false,
    val typeAnalCreampie: Boolean = false,
    val isPregnancyMarked: Boolean = false,

    // Novos Campos Exclusivos para Atendimentos Profissionais (Aba GP)
    val gpSexoVaginal: Boolean = false,
    val gpOralSemCamisinha: Boolean = false,
    val gpSexoAnal: Boolean = false,
    val gpBeijoNaBoca: Boolean = false,        // Em substituição a práticas sem camisinha das fichas comuns
    val gp69ComCamisinha: Boolean = false,
    val gpMassagemErotica: Boolean = false,
    val gpDominacaoBdsm: Boolean = false,
    val gpBeijoGrego: Boolean = false,
    val gpInversaoFetiche: Boolean = false,
    val gpGargantaProfunda: Boolean = false,
    val gpFetichePe: Boolean = false,
    val gpFioTerra: Boolean = false,
    val gpGastoValor: Double = 0.0            // Valor financeiro pago pelo encontro/fetiches extra
)
