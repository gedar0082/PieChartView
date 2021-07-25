package com.gedar0082.someview

import android.content.Context
import android.content.res.Configuration
import android.content.res.TypedArray
import android.graphics.*
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View

import java.util.*


class ChartView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    //значения по умолчанию
    //
    //толщина обводки
    var outlineWidth = 5f

    //цвет обводки
    var outlineColor = Color.RED

    //данные по умолчанию
    var data = doubleArrayOf(1.0, 2.0, 3.0)
        set(value) {
            field = value
            //сумма входных данных для их нормализации
            val sumOf = data.sum()
            //переводим данные в ожидаемые углы
            data.forEach { anglesArray.add((it / sumOf * 100) / 100 * 360); piesPaths.add(Path()) }
        }

    //ориентация девайса (по умолчанию вертикальная)
    private var orientation = Configuration.ORIENTATION_PORTRAIT

    //объекты, использующиеся в draw инициализирую здесь чтобы не тратилось время
    // на выделение памяти в onDraw
    //
    //путь для рисования обводки
    private val circlePath: Path = Path()

    //массив путей для рисования частей графика. Переиспользовать один путь не получилось,
    // поэтому создал сразу их по числу частей графика.
    private val piesPaths = mutableListOf<Path>()

    //для рисования линий
    private val linePaint = Paint()

    //для заливки цветом. Решил менять цвет в цикле вместо инициализации кучи объектов.
    private val fillPaint = Paint()

    //для установки радиуса
    private var radius = 0f

    //массив углов в градусах, которые я получу после нормализации
    // первичных данных и перевода в углы
    private var anglesArray = mutableListOf<Double>()

    //объект для прямоугольника для рисования дуг
    private val rect = RectF()

    //объект для рандома, используется только для генерации цвета
    private val rnd: Random = Random()

    //объект для подсчета пройденных углов при отрисовке частей графика
    private var currentAngle = 0f

    init {
        //перевод dp в пиксели
        val displayMetrics: DisplayMetrics = context!!.resources.displayMetrics
        orientation = context.resources.configuration.orientation
        //задаём значения по умолчанию
        var outlineWidthFromAttr: Int =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, outlineWidth, displayMetrics)
                .toInt()
        var outlineColorFromAttr: Int = outlineColor

        //проверяем наличие значений в аттрибутах
        if (attrs != null) {
            val typedArray: TypedArray =
                context.obtainStyledAttributes(attrs, R.styleable.ChartView)
            outlineWidthFromAttr = typedArray.getDimensionPixelSize(
                R.styleable.ChartView_outlineWidth,
                outlineWidthFromAttr
            )
            outlineColorFromAttr =
                typedArray.getColor(R.styleable.ChartView_outlineColor, outlineColorFromAttr)
            typedArray.recycle()
        }

        //заполняем значения для рисовальщика полученными(или по-умолчанию) значениями
        outlineWidth = outlineWidthFromAttr.toFloat()
        outlineColor = outlineColorFromAttr

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        //запрашиваем разрешенные размеры
        //в целом размеры моей вьюшки не зависят от наполнения так что просто добавляю паддиги
        val measureSpecWidth = MeasureSpec.getSize(widthMeasureSpec)
        val measureSpecHeight = MeasureSpec.getSize(heightMeasureSpec)

        //учитываем паддинги
        val leftPadding = paddingLeft
        val rightPadding = paddingRight
        val topPadding = paddingTop
        val bottomPadding = paddingBottom

        //добавляем паддинги в ширину и длину
        val width = measureSpecWidth + leftPadding + rightPadding
        val height = measureSpecHeight + topPadding + bottomPadding

        //проверяем оринтацию девайса. Вьюшка должна быть квадратной. Если оринтация вертикальная,
        // то ровняем её по ширине, если горизонтальная - то по высоте
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            setMeasuredDimension(width, width)
        } else {
            setMeasuredDimension(height, height)
        }

    }

    override fun onDraw(canvas: Canvas) {

        //заполняем рисовальщик
        //он заполняется здесь, так как если заполнять в конструкторе,
        // то из кода потом значения не поменять
        linePaint.style = Paint.Style.STROKE
        linePaint.color = outlineColor
        linePaint.strokeWidth = outlineWidth
        fillPaint.style = Paint.Style.FILL
        fillPaint.color = outlineColor

        //сбрасываю путь построения круга
        circlePath.reset()
        //задаю радиус, который является половиной от размера вьюшки.
        // При построении круга дополнительно вычитаю толщину обводки.
        radius = measuredHeight / 2f

        //перемещаюсь к центру будущей окружности
        circlePath.moveTo(measuredHeight / 2.toFloat(), measuredWidth / 2.toFloat())
        //рисую круг
        circlePath.addCircle(
            measuredHeight / 2f,
            measuredWidth / 2f,
            radius - outlineWidth,
            Path.Direction.CCW
        )
        //ограничиваю область рисования этим кругом
        canvas.clipPath(circlePath)
        //рисую круг заливкой(этого никто не увидит, но на всякий случай она красная )
        canvas.drawPath(circlePath, fillPaint)

        //создание прямоугольника для построения дуги
        rect.apply {
            left = 0f
            top = 0f
            right = measuredWidth.toFloat()
            bottom = measuredHeight.toFloat()
        }

        //построение частей графа
        for (i in 0 until anglesArray.size) {
            //выбор пути новой части
            piesPaths[i].apply {
                //сбрасываю путь построения части
                reset()
                //переход к центру окружности
                moveTo(measuredWidth / 2.toFloat(), measuredHeight / 2.toFloat())
                //построение дуги от предыдущего угла(по умолчанию в первом заходе 0)
                addArc(rect, currentAngle, anglesArray[i].toFloat())
                //построение линии к центру, этого достаточно чтобы сделать заливку
                lineTo(measuredHeight / 2.toFloat(), measuredWidth / 2.toFloat())
            }
            //отрисовка этого пути на полотне
            canvas.drawPath(piesPaths[i], fillPaint.apply {
                style = Paint.Style.FILL
                //генерю рандомный цвет. дурацкая идея, но её хватает
                color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256),
                    rnd.nextInt(256))
            })
            //добавляю пройденный угол к сумме, чтобы потом от неё строить следующие дуги
            currentAngle += anglesArray[i].toFloat()
        }
        //после выхода из цикла отрицовка контура
        canvas.drawPath(circlePath, linePaint)
    }

}