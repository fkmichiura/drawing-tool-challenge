package br.com.saedigital.drawingapp

import io.realm.RealmList
import io.realm.RealmModel
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass

@RealmClass
open class DrawCoordinate : RealmModel {

    var x: Float = 0f
    var y: Float = 0f
    var x2: Float = 0f
    var y2: Float = 0f
    var drag: Int = 0
}

@RealmClass
open class Draw : RealmModel {

    @PrimaryKey
    var id: Int = 0
    var tool: Int = 0
    var shape: Float = 0F
    var fill: Boolean = false
    var canDraw: Boolean = true
    var color: String = ""
    var coordinates: RealmList<DrawCoordinate> = RealmList()
}