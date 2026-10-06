package com.example.shellconsole.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.shellconsole.ui.component.BottomBar
import com.example.shellconsole.ui.component.MainDestination
import com.example.shellconsole.ui.component.MainPagerState
import com.example.shellconsole.ui.component.NavigationBadgeState
import com.example.shellconsole.ui.component.SideRail
import com.example.shellconsole.ui.component.rememberMainPagerState
import com.example.shellconsole.ui.screen.HomePage
import com.example.shellconsole.ui.screen.ModulePage
import com.example.shellconsole.ui.screen.SuperUserPage
import com.example.shellconsole.ui.screen.settings.AppearanceActions
import com.example.shellconsole.ui.screen.settings.AppearanceState
import com.example.shellconsole.ui.screen.settings.SettingsPage
import com.example.shellconsole.ui.screen.settings.bottomBarBlurEnabled

/**
 * 各主页面内部用这套安全区：只吃"顶部 + 左右"。
 *
 * 底部刻意排除 —— 底部由 MainScreen 以 bottomInnerPadding 的形式传给页面，
 * 这样页面内容可以一直绘制到导航栏底下（滚动内容从导航栏后穿过），
 * 而不是被硬生生截断在导航栏上沿。这正是 KernelSU 的做法。
 *
 * 注意：WindowInsets.systemBars 与 .only() 都是 @Composable 取值器/函数，
 * 不能在顶层 val 初始化时直接读，所以这里写成同名的惰性 getter。
 */
internal val PageContentWindowInsets: WindowInsets
    @Composable
    get() = WindowInsets.systemBars.only(
        WindowInsetsSides.Horizontal + WindowInsetsSides.Top
    )

/** 底栏毛玻璃的模糊半径 */
private val BarBlurRadius = 24.dp

/** 悬浮底栏的内边距（圆角半径见 [FloatingBarCorner]） */
private val FloatingBarCorner = 28.dp

/**
 * 毛玻璃状态下底栏底色的不透明度。
 * 必须留出透明度，否则背后的模糊根本透不出来 —— 这是"毛玻璃"和"纯色底栏"的唯一区别。
 */
private const val BarGlassAlpha = 0.55f

/**
 * ============================================================
 *  主界面外壳（对标 KernelSU Manager 的 MainActivity + MainScreen）
 * ============================================================
 *  自上而下就是 KernelSU 的那套结构：
 *    ① 底色用 surfaceContainer（比 surface 略深一档，让 TonalCard 浮起来）
 *    ② HorizontalPager 四页，可左右滑动切换
 *    ③ 手机/窄屏：底部导航栏（可悬浮、可毛玻璃）；宽屏：左侧导航栏
 *    ④ 返回键：不在首页时先回首页，而不是直接退出应用
 *
 *  【为什么页码与导航项用同一个枚举驱动】
 *  MainDestination 的 ordinal 既是 Pager 的页码，也是导航栏的下标，
 *  增删页面时只需改枚举一处，不会出现"导航四项、页面三个"这种错位。
 * ============================================================
 *
 * @param viewModel 控制台业务状态（Activity 作用域，跨页共享且旋转不丢）
 * @param appearance 外观设置状态（明暗模式 / 主色 / 页面缩放 / 底栏形态）
 * @param actions    外观设置的回调
 */
@Composable
fun MainScreen(
    viewModel: ConsoleViewModel,
    appearance: AppearanceState,
    actions: AppearanceActions,
) {
    val pagerState = rememberPagerState(pageCount = { MainDestination.PAGE_COUNT })
    val mainPagerState = rememberMainPagerState(pagerState)

    // 本 App 暂无"已授权应用数 / 模块更新数"这类计数，角标恒为空。
    // 结构保留，接入功能后把真实数字塞进来即可，导航栏组件无需改动。
    val badgeState = NavigationBadgeState()

    // 手指滑动结束后，把真实页码同步回导航栏高亮；
    // 点击导航栏触发的动画期间由 MainPagerState 自己兜住，不会来回跳。
    val currentPage = pagerState.currentPage
    LaunchedEffect(currentPage) {
        mainPagerState.syncPage()
    }

    // 返回键：不在首页时先回首页（与 KernelSU 一致），回到首页后再按才退出应用
    BackHandler(enabled = mainPagerState.selectedPage != MainDestination.HOME.ordinal) {
        mainPagerState.animateToPage(MainDestination.HOME.ordinal)
    }

    // 软键盘弹起时隐藏底部导航栏。
    // 不这么做的话，控制台输入行会被"导航栏 + 键盘"两段高度一起挤掉。
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0

    val pagerContent: @Composable (Dp) -> Unit = { bottomInnerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (page) {
                MainDestination.HOME.ordinal -> HomePage(viewModel, bottomInnerPadding)
                MainDestination.SUPERUSER.ordinal -> SuperUserPage(bottomInnerPadding)
                MainDestination.MODULE.ordinal -> ModulePage(bottomInnerPadding)
                else -> SettingsPage(
                    appearance = appearance,
                    actions = actions,
                    bottomInnerPadding = bottomInnerPadding,
                )
            }
        }
    }

    // 用 BoxWithConstraints 而不是读全局窗口尺寸：分屏、折叠屏、自由窗口改变大小时
    // 能立刻重新判定宽窄，不会等到重建 Activity
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (useSplitPane(maxWidth, maxHeight)) {
            // ---------- 宽屏：左侧导航栏 ----------
            val navBarBottomPadding =
                WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

            Scaffold(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                // 安全区不在这里吃：侧边栏自己吃"起点侧+垂直"，各页面自己吃"顶部+左右"。
                // 若这里也加，就会和页面重复计算，顶部/左侧出现双倍留白。
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
            ) { _ ->
                Row(modifier = Modifier.fillMaxSize()) {
                    SideRail(badgeState = badgeState, mainPagerState = mainPagerState)
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        pagerContent(navBarBottomPadding)
                    }
                }
            }
        } else {
            // ---------- 手机 / 窄屏：底部导航栏 ----------
            PhoneLayout(
                appearance = appearance,
                mainPagerState = mainPagerState,
                badgeState = badgeState,
                imeVisible = imeVisible,
                pagerContent = pagerContent,
            )
        }
    }
}

/**
 * ============================================================
 *  手机/窄屏布局：内容 + 底部导航栏
 * ============================================================
 *  【为什么不用 Scaffold 的 bottomBar 槽位】
 *  要做"毛玻璃"，就必须把**底栏背后的内容**模糊后垫在底栏下面，也就是需要
 *  一份内容的副本。Compose 里没有"取背景快照"的 API，可行做法是把内容录进一个
 *  GraphicsLayer，再在底栏区域裁剪着画出来（这就是 Haze 之类库的原理）。
 *
 *  而 Scaffold 的 bottomBar 是独立槽位，拿不到内容层、坐标系也对不上。
 *  所以这里改成手写结构：外层一个 Box 同时容纳"内容层"和"底栏"，
 *  两者共享同一坐标系，底栏就能精确地把自己那片区域的内容模糊后垫在身下。
 *  悬浮底栏本来就是靠"内容从底栏下方穿过"才有意义，两者正好一致。
 * ============================================================
 */
@Composable
private fun PhoneLayout(
    appearance: AppearanceState,
    mainPagerState: MainPagerState,
    badgeState: NavigationBadgeState,
    imeVisible: Boolean,
    pagerContent: @Composable (Dp) -> Unit,
) {
    val density = LocalDensity.current
    val floating = appearance.floatingBottomBar
    // 键盘弹起时底栏本来就要隐藏，自然也不需要模糊
    val blurOn = !imeVisible && appearance.bottomBarBlurEnabled
    val blurRadiusPx = with(density) { if (blurOn) BarBlurRadius.toPx() else 0f }

    // 内容会被录进这个图层，供底栏取用。
    // 【关键】这一层绝不能带 renderEffect：它同时还要被 drawLayer 画回屏幕，
    // 一旦挂上模糊，整屏内容都会跟着糊掉。模糊由底栏自己的独立图层负责（见 BottomBarBackdrop）。
    val backdropLayer = rememberGraphicsLayer()

    var contentCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var barCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var barHeightPx by remember { mutableIntStateOf(0) }

    // 底栏在内容坐标系里的位置。图层是按内容坐标系录的，画到"底栏自己"的坐标系里
    // 必须先反向平移这么多，否则会出现"模糊的是别处画面"的错位。
    val barOffsetInContent = if (contentCoords != null && barCoords != null) {
        contentCoords!!.localPositionOf(barCoords!!, Offset.Zero)
    } else {
        Offset.Unspecified
    }

    // 给页面留出的底部空间：底栏占多高就留多高（悬浮时含外边距），
    // 这样滚动内容的末尾不会被底栏永久挡住
    val barReservedHeight = if (imeVisible) 0.dp else with(density) { barHeightPx.toDp() }

    val barShape = if (floating) RoundedCornerShape(FloatingBarCorner) else RectangleShape

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        // ---------- ① 内容层：录进图层后照常绘制 ----------
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { contentCoords = it }
                .drawWithContent {
                    backdropLayer.record(
                        density = this,
                        layoutDirection = this.layoutDirection,
                        size = IntSize(size.width.toInt(), size.height.toInt()),
                    ) {
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(backdropLayer)
                },
        ) {
            pagerContent(barReservedHeight)
        }

        // ---------- ② 底栏（贴底，画在内容之上）----------
        if (!imeVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onSizeChanged { barHeightPx = it.height },
            ) {
                BottomBarBackdrop(
                    modifier = Modifier
                        .then(
                            if (floating) {
                                Modifier.padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 8.dp,
                                    bottom = 8.dp,
                                )
                            } else {
                                Modifier
                            }
                        )
                        .fillMaxWidth()
                        .onGloballyPositioned { barCoords = it },
                    sourceLayer = if (blurOn) backdropLayer else null,
                    offsetInContent = barOffsetInContent,
                    blurRadiusPx = blurRadiusPx,
                    shape = barShape,
                ) {
                    BottomBar(
                        badgeState = badgeState,
                        mainPagerState = mainPagerState,
                        modifier = if (floating) Modifier.clip(barShape) else Modifier,
                        containerColor = if (blurOn) {
                            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = BarGlassAlpha)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                    )
                }
            }
        }
    }
}

/**
 * 底栏的毛玻璃底衬：在自己这块圆角区域里，把内容副本模糊着画一遍。
 *
 * 【为什么必须用两个图层】
 * 内容层（sourceLayer）录完之后还要被 `drawLayer` 画回屏幕正常显示，
 * 所以它自己**不能**挂 renderEffect —— 挂了整屏就糊了。
 * 这里另开一层 blurLayer：只把"平移对齐后的内容"录进去，模糊效果加在这一层上，
 * 再裁剪到底栏的圆角范围画出来。两个图层各司其职，屏幕上的内容始终是清晰的。
 *
 * @param sourceLayer     内容图层（无效果）；为 null 表示不做毛玻璃，本组件退化成普通容器
 * @param offsetInContent 底栏左上角在内容坐标系中的位置，用 [Offset.Unspecified] 表示尚未测量到
 * @param blurRadiusPx    模糊半径（像素）；0 表示不模糊
 * @param shape           裁剪与圆角形状
 */
@Composable
private fun BottomBarBackdrop(
    modifier: Modifier,
    sourceLayer: GraphicsLayer?,
    offsetInContent: Offset,
    blurRadiusPx: Float,
    shape: Shape,
    content: @Composable () -> Unit,
) {
    // 专供模糊的一层；半径变化时才更新效果，避免每帧都改渲染节点
    val blurLayer = rememberGraphicsLayer()
    LaunchedEffect(blurRadiusPx) {
        blurLayer.renderEffect = if (blurRadiusPx > 0f) {
            BlurEffect(blurRadiusPx, blurRadiusPx, TileMode.Clamp)
        } else {
            null
        }
    }

    Box(
        modifier = modifier.drawWithContent {
            val source = sourceLayer
            if (source != null && blurRadiusPx > 0f && offsetInContent != Offset.Unspecified) {
                // ① 把内容副本按底栏所在位置平移后录进模糊层
                blurLayer.record(
                    density = this,
                    layoutDirection = this.layoutDirection,
                    size = IntSize(size.width.toInt(), size.height.toInt()),
                ) {
                    translate(left = -offsetInContent.x, top = -offsetInContent.y) {
                        drawLayer(source)
                    }
                }
                // ② 裁到底栏的圆角范围内画出来
                //    注意密度参数要显式指向 drawWithContent 的 DrawScope：
                //    这一行在 Path().apply{} 里，裸写 this 会解析成那个 Path。
                val outline = shape.createOutline(size, layoutDirection, this@drawWithContent)
                clipPath(Path().apply { addOutline(outline) }) {
                    drawLayer(blurLayer)
                }
            }
            // 底栏本体画在模糊底衬之上；它的底色带透明度，模糊才透得出来
            drawContent()
        },
    ) {
        content()
    }
}

/**
 * 宽屏判定，规则与 KernelSU 的 shouldShowSplitPane() 完全一致：
 *   - 宽度 ≥ 840dp            → 用侧边栏（平板竖屏、横屏、桌面模式都覆盖）
 *   - 宽度 ≥ 600dp 且高宽比 < 1.2 → 用侧边栏（平板横屏这种"扁而宽"的形态）
 * 手机竖屏（< 600dp）与手机横屏（高宽比 ≥ 1.2）继续用底部导航栏。
 */
private fun useSplitPane(width: Dp, height: Dp): Boolean =
    width >= 840.dp || (width >= 600.dp && height / width < 1.2f)
