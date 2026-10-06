package com.example.shellconsole.ui.component

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

/**
 * ============================================================
 *  主页面容器状态（对标 KernelSU Manager 的 MainPagerState）
 * ============================================================
 *  【为什么需要它】
 *  HorizontalPager 只认识"当前页"，而底部导航还要知道"用户点了哪一页"。
 *  这两者在动画期间会短暂不一致：手指滑到一半、或点击后动画还没走完时，
 *  Pager 的 currentPage 与导航栏高亮必须解耦，否则导航栏图标会来回跳。
 *
 *  这个类把两者粘起来：
 *    - [selectedPage] ：导航栏高亮依据。点击时**立刻**改变，不等动画结束。
 *    - [isNavigating] ：翻页动画进行中为 true，此期间不接受 Pager 回调的反向覆盖。
 *    - [syncPage]     ：手指滑动结束后，把 Pager 的真实页码同步回来。
 * ============================================================
 */
@Stable
class MainPagerState(
    val pagerState: PagerState,
    private val coroutineScope: CoroutineScope,
) {

    /** 当前选中的页（导航栏高亮依据） */
    var selectedPage by mutableIntStateOf(pagerState.currentPage)
        private set

    /** 是否正在执行"点击导航栏"触发的翻页动画 */
    var isNavigating by mutableStateOf(false)
        private set

    private var navJob: Job? = null

    /** 点击导航栏 / 返回键：平滑滚到目标页 */
    fun animateToPage(targetIndex: Int) {
        if (targetIndex == selectedPage) return

        // 连续点击时取消上一次动画，以最后一次为准，避免两个动画互相拉扯
        navJob?.cancel()
        selectedPage = targetIndex
        isNavigating = true

        navJob = coroutineScope.launch {
            val myJob = coroutineContext.job
            try {
                pagerState.animateScrollToPage(targetIndex)
            } finally {
                // 只有"仍然是本次动画"才收尾；被取消的旧动画不许再改状态
                if (navJob == myJob) {
                    isNavigating = false
                    // 动画被打断（例如用户中途手动滑走）时，以 Pager 的真实页码为准
                    if (pagerState.currentPage != targetIndex) {
                        selectedPage = pagerState.currentPage
                    }
                }
            }
        }
    }

    /** 手指滑动结束后同步页码；动画进行中不覆盖，否则导航栏会闪 */
    fun syncPage() {
        if (!isNavigating && selectedPage != pagerState.currentPage) {
            selectedPage = pagerState.currentPage
        }
    }
}

@Composable
fun rememberMainPagerState(
    pagerState: PagerState,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): MainPagerState = remember(pagerState, coroutineScope) {
    MainPagerState(pagerState, coroutineScope)
}
