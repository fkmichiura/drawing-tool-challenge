package br.com.saedigital.drawingapp

import android.app.AlertDialog
import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

class TouchCustomView : View {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        initializeDrawingView()
    }

    //Ferramentas
    companion object {
        const val PENCIL = 1
        const val LINE = 2
        const val RECTANGLE = 3
        const val CIRCLE = 4
        const val ERASE = 5
    }

    private var drawPath: Path? = null
    private var drawPaint: Paint? = null
    private var canvasPaint: Paint? = null
    private var fillPaint: Paint? = null
    private var strokePaint: Paint? = null
    private var canvasBitmap: Bitmap? = null
    private var drawCanvas: Canvas? = null
    private var start = PointF()
    private val m = PointF()

    private var coordDraw: Draw? = null
    private var selectedDraw: Draw? = null

    private var drawCoordinate: DrawCoordinate? = null

    private var tool: Int = 0
    private var shape: Float = 0.0f
    var selectedShape: Float = 1.0f
        get() {
            return if(selectedDraw == null) shape else selectedDraw!!.shape
        }
    private var drawColor: String = "0000FF"
    var selectedFill: Boolean = false
    get() {
        return if(selectedDraw == null) fill else selectedDraw!!.fill
    }
    private var fill: Boolean = false
    private var isDrawing: Boolean = false

    private fun initializeDrawingView() {

        drawPath = Path()

        drawCanvas = Canvas()

        canvasPaint = Paint(Paint.DITHER_FLAG)
        canvasPaint!!.isAntiAlias = true
        canvasPaint!!.isDither = true
        canvasPaint!!.style = Paint.Style.STROKE
        canvasPaint!!.strokeJoin = Paint.Join.ROUND
        canvasPaint!!.strokeCap = Paint.Cap.ROUND
        canvasPaint!!.strokeWidth = shape

        drawPaint = Paint(Paint.DITHER_FLAG)
        drawPaint!!.isAntiAlias = true
        drawPaint!!.isDither = true
        drawPaint!!.style = Paint.Style.STROKE
        drawPaint!!.strokeJoin = Paint.Join.ROUND
        drawPaint!!.strokeCap = Paint.Cap.ROUND
        drawPaint!!.strokeWidth = shape

        //Fill e Stroke
        fillPaint = Paint(Paint.DITHER_FLAG)
        fillPaint!!.isAntiAlias = true
        fillPaint!!.isDither = true
        fillPaint!!.style = Paint.Style.FILL
        fillPaint!!.strokeJoin = Paint.Join.ROUND
        fillPaint!!.strokeCap = Paint.Cap.ROUND
        fillPaint!!.strokeWidth = shape

        strokePaint = Paint(Paint.DITHER_FLAG)
        strokePaint!!.isAntiAlias = true
        strokePaint!!.isDither = true
        strokePaint!!.style = Paint.Style.STROKE
        strokePaint!!.strokeJoin = Paint.Join.ROUND
        strokePaint!!.strokeCap = Paint.Cap.ROUND
        strokePaint!!.strokeWidth = 1F

        initTool()
    }

    var list: List<Draw>? = null

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (canvas != null && canvasBitmap != null) {

            if (list == null) {
                isDrawing = true

                list = DataManager.getDrawList()

                for (draw in list!!) {
                    selectedDraw = draw

                    for(coordinate in draw.coordinates) {

                        drawCoordinate = coordinate

                        when (coordinate.drag) {
                            0 -> onTouchEventActionDown()
                            1 -> onTouchEventActionMove()
                            2 -> onTouchEventActionUp()
                        }
                        drawTool(canvas)
                    }
                }
                selectedDraw = null
                drawCoordinate = null
                return
            }

            if (isDrawing) {
                drawTool(canvas)
            }
        }
    }

    fun drawTool(canvas: Canvas) {

        val toolType = if(selectedDraw == null) tool else selectedDraw!!.tool

        when (toolType) {

            PENCIL -> onDrawPencil(canvas)

            LINE -> onDrawLine(canvas)

            RECTANGLE -> onDrawRectangle(canvas)

            CIRCLE -> onDrawCircle(canvas)

            ERASE -> onEraseDraw(canvas)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        m.x = event.x
        m.y = event.y

        when (event.action) {

            MotionEvent.ACTION_DOWN -> {
                isDrawing = true
                onTouchEventActionDown()
            }

            MotionEvent.ACTION_MOVE -> {
                onTouchEventActionMove()
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                onTouchEventActionUp()
                drawFromDb()

                isDrawing = false
            }
        }
        return true
    }

    //------------------------------------------------------------------
    // Eventos de Touch
    //------------------------------------------------------------------
    private fun onTouchEventActionDown() {

        if (drawCoordinate == null) {
            setColor(drawColor)

            start.x = m.x
            start.y = m.y

            coordDraw = DataManager.createDraw(tool, shape, true, drawColor, fill)

            if(tool == PENCIL || tool == ERASE)
                DataManager.createCoordinate(draw = coordDraw!!, coord = m, coordDrag = 0)
        }
        else{
            setColor(selectedDraw!!.color)

            start.x = drawCoordinate!!.x
            start.y = drawCoordinate!!.y
        }

        if ((selectedDraw != null && selectedDraw!!.tool == PENCIL) || tool == PENCIL) {
            drawPath!!.reset()

            if (drawCoordinate == null) {
                drawPath!!.moveTo(m.x, m.y)
            }
            else
                drawPath!!.moveTo(drawCoordinate!!.x, drawCoordinate!!.y)
        }
    }

    private fun onTouchEventActionMove() {

        val t = selectedDraw?.tool ?: tool

        if(t == PENCIL || t == ERASE){

            if (drawCoordinate == null) {
                setColor(drawColor)
                DataManager.createCoordinate(draw = coordDraw!!, coord = m, coordDrag = 1)
                drawPath!!.lineTo(m.x, m.y)
            }
            else {
                setColor(selectedDraw!!.color)
                drawPath!!.lineTo(drawCoordinate!!.x, drawCoordinate!!.y)
            }
        }
    }

    private fun onTouchEventActionUp(): Boolean {

        when (selectedDraw?.tool ?: tool) {

            PENCIL -> {
                setErase(false)
                if (drawCoordinate == null) {
                    setColor(drawColor)
                    DataManager.createCoordinate(draw = coordDraw!!, coord = m, coordDrag = 2)
                }
                else{
                    setColor(selectedDraw!!.color)
                }


                drawCanvas?.drawPath(drawPath, drawPaint)
                drawPath!!.reset()
                return true
            }

            LINE -> {
                setErase(false)
                if (drawCoordinate == null){
                    setColor(drawColor)
                    DataManager.createCoordinate(draw = coordDraw!!, coord = start, coord2 = m, coordDrag = 2)
                }
                else{
                    setColor(selectedDraw!!.color)
                }

                onDrawLine(drawCanvas!!)
                return true
            }

            RECTANGLE -> {
                setErase(false)
                if (drawCoordinate == null){
                    setColor(drawColor)
                    DataManager.createCoordinate(draw = coordDraw!!, coord = start, coord2 = m, coordDrag = 2)
                }
                else{
                    setColor(selectedDraw!!.color)
                }

                onDrawRectangle(drawCanvas!!)
                return true
            }

            CIRCLE -> {
                setErase(false)
                if (drawCoordinate == null){
                    setColor(drawColor)
                    DataManager.createCoordinate(draw = coordDraw!!, coord = start, coord2 = m, coordDrag = 2)
                }
                else{
                    setColor(selectedDraw!!.color)
                }

                onDrawCircle(drawCanvas!!)
                return true
            }

            ERASE -> {
                setErase(true)
                if (drawCoordinate == null)
                    DataManager.createCoordinate(draw = coordDraw!!, coord = m, coordDrag = 2)

                drawCanvas?.drawPath(drawPath, drawPaint)
                drawPath!!.reset()
                return true
            }
        }
        return false
    }

    //------------------------------------------------------------------
    // Lápis
    //------------------------------------------------------------------
    private fun onDrawPencil(canvas: Canvas) {
        setShape(selectedShape)

        canvas.drawPath(drawPath, drawPaint)
    }

    //------------------------------------------------------------------
    // Linha
    //------------------------------------------------------------------
    private fun onDrawLine(canvas: Canvas) {
        setShape(selectedShape)

        val pointX: Float = if(drawCoordinate == null) m.x else drawCoordinate!!.x2
        val pointY: Float = if(drawCoordinate == null) m.y else drawCoordinate!!.y2
        val pointX2: Float = if(drawCoordinate == null) start.x else drawCoordinate!!.x
        val pointY2: Float = if(drawCoordinate == null) start.y else drawCoordinate!!.y

        canvas.drawLine(pointX2, pointY2, pointX, pointY, drawPaint)
    }

    //------------------------------------------------------------------
    // Retângulo
    //------------------------------------------------------------------
    private fun onDrawRectangle(canvas: Canvas) {
        setShape(selectedShape)

        val pointX: Float = if(drawCoordinate == null) m.x else drawCoordinate!!.x2
        val pointY: Float = if(drawCoordinate == null) m.y else drawCoordinate!!.y2
        val pointX2: Float = if(drawCoordinate == null) start.x else drawCoordinate!!.x
        val pointY2: Float = if(drawCoordinate == null) start.y else drawCoordinate!!.y

        val left = if (pointX2 > pointX) pointX else pointX2
        val top = if (pointY2 > pointY) pointY else pointY2
        val right = if (pointX2 > pointX) pointX2 else pointX
        val bottom = if (pointY2 > pointY) pointY2 else pointY

        if (selectedFill) {
            canvas.drawRect(left, top, right, bottom, strokePaint)
            canvas.drawRect(left, top, right, bottom, fillPaint)
        }
        else{
            canvas.drawRect(left, top, right, bottom, drawPaint)
        }
    }

    //------------------------------------------------------------------
    // Círculo
    //------------------------------------------------------------------
    private fun onDrawCircle(canvas: Canvas) {
        setShape(selectedShape)

        val pointX: Float = if(drawCoordinate == null) m.x else drawCoordinate!!.x2
        val pointY: Float = if(drawCoordinate == null) m.y else drawCoordinate!!.y2
        val pointX2: Float = if(drawCoordinate == null) start.x else drawCoordinate!!.x
        val pointY2: Float = if(drawCoordinate == null) start.y else drawCoordinate!!.y

        val left = if (pointX2 > pointX) pointX else pointX2
        val top = if (pointY2 > pointY) pointY else pointY2
        val right = if (pointX2 > pointX) pointX2 else pointX
        val bottom = if (pointY2 > pointY) pointY2 else pointY

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            if (selectedFill) {
                canvas.drawOval(left, top, right, bottom, strokePaint)
                canvas.drawOval(left, top, right, bottom, fillPaint)
            }
            else {
                canvas.drawOval(left, top, right, bottom, drawPaint)
            }
        }
        else {
            val oval = RectF(left, top, right, bottom)

            if (selectedFill) {
                canvas.drawOval(oval, strokePaint)
                canvas.drawOval(oval, fillPaint)
            }
            else {
                canvas.drawPath(drawPath, drawPaint)
            }
        }
    }

    //------------------------------------------------------------------
    // Borracha
    //------------------------------------------------------------------
    private fun onEraseDraw(canvas: Canvas) {

        val pointX: Float = if(drawCoordinate == null) m.x else drawCoordinate!!.x2
        val pointY: Float = if(drawCoordinate == null) m.y else drawCoordinate!!.y2
        val pointX2: Float = if(drawCoordinate == null) start.x else drawCoordinate!!.x
        val pointY2: Float = if(drawCoordinate == null) start.y else drawCoordinate!!.y

        if (selectedFill) {
            canvas.drawRect(pointX2, pointY2, pointX, pointY, drawPaint)
            canvas.drawRect(pointX2, pointY2, pointX, pointY, fillPaint)
        }
        else {
            canvas.drawLine(pointX2, pointY2, pointX, pointY, drawPaint)
        }
    }

    //Altera a cor da linha a ser desenhada
    fun setColor(newColor: String) {

        val paintColor = Color.parseColor("#$newColor")
        drawPaint!!.color = paintColor
        fillPaint!!.color = paintColor
    }

    //Altera a espessura da linha a ser desenhada
    fun setShape(newSize: Float) {

        val pixelAmount = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                newSize, resources.displayMetrics)

        drawPaint!!.strokeWidth = 2 * pixelAmount
    }

    //Borracha
    fun setErase(isErase: Boolean) {

        if (isErase) {
            drawPaint!!.alpha = 0
            drawPaint!!.strokeWidth = shape
            drawPaint!!.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)

            if (selectedFill) {
                fillPaint!!.alpha = 0
                fillPaint!!.strokeWidth = 1F
                fillPaint!!.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                drawPaint!!.strokeWidth = 1F
            }
            else
                drawPaint!!.strokeWidth = shape
        }
        else {
            drawPaint!!.xfermode = null
            fillPaint!!.xfermode = null
        }
    }

    //Apagar todos os desenhos
    fun setDeleteAllDialog(context: Context) {

        val newDialog = AlertDialog.Builder(context)
        newDialog.setTitle("Atenção")
        newDialog.setMessage("Deseja apagar todo o conteúdo editado?")
        newDialog.setPositiveButton("Sim") { dialog, _ ->
            drawCanvas!!.drawColor(0, PorterDuff.Mode.CLEAR)
            invalidate()
            dialog.dismiss()
        }
        newDialog.setNegativeButton("Não") { dialog, _ -> dialog.cancel() }
        newDialog.show()
    }

    fun initTool() {

        drawColor = "db1717"
        shape = 4.0F
        fill = false

        if (this.width > 0 && this.height > 0) {
            canvasBitmap = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)

            drawFromDb()
        }
        setColor(drawColor)
        setShape(shape)
        setErase(false)
        tool = ERASE
    }

    private fun drawFromDb() {

        list = null
        invalidate()

//        val list = DataManager.getDrawList()
//
//        isDrawing = true
//
//        for(item in list){
//
//            when(item.drag){
//                0 -> onTouchEventActionDown(item.coordX, item.coordY)
//                1 -> onTouchEventActionMove(item.coordX, item.coordY)
//                2 -> onTouchEventActionUp(item.coordX, item.coordY)
//            }
//        }
    }
}