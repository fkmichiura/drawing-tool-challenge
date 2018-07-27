package br.com.saedigital.drawingapp

import android.graphics.PointF
import android.util.Log
import io.realm.Realm

object DataManager {

    fun createDraw(drawTool: Int, drawShape: Float, drawCan: Boolean, drawColor: String, drawFill: Boolean): Draw {

        val draw = Draw()

        realm {

            val db = it.where(Draw::class.java).findAll()

            draw.id = db.size
            draw.tool = drawTool
            draw.fill = drawFill
            draw.shape = drawShape
            draw.canDraw = drawCan
            draw.color = drawColor

            it.copyToRealmOrUpdate(draw)

            Log.d("DB Log", "createDraw")
        }
        return draw
    }

    fun createCoordinate(draw: Draw, coord: PointF, coord2: PointF? = null, coordDrag: Int) {

        realm {
            val coordinate = DrawCoordinate()

            with(coordinate) {
                x = coord.x
                y = coord.y
                drag = coordDrag
            }

            if (coord2 != null) {

                with(coordinate) {
                    x2 = coord2.x
                    y2 = coord2.y
                }
            }

            draw.coordinates.add(coordinate)
            it.copyToRealmOrUpdate(draw)

            Log.d("DB Log", "createCoordinate")
        }
    }

    fun getDrawList(): List<Draw> {

        val realm = Realm.getDefaultInstance()
        val dbDraw = realm.where(Draw::class.java).findAll()
        val resultList: MutableList<Draw> = mutableListOf()

        if (dbDraw != null && dbDraw.isNotEmpty()) {

            for (draw in dbDraw) {

                resultList.add(draw)
            }
        }
        return resultList
    }

    private fun realm(handler: (it: Realm) -> Unit) {

        val realm = Realm.getDefaultInstance()

        realm.beginTransaction()

        handler(realm)

        realm.commitTransaction()
        realm.close()
    }
}