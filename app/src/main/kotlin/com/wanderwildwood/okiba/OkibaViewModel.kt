package com.wanderwildwood.okiba

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.wanderwildwood.okiba.core.Compass
import com.wanderwildwood.okiba.core.Geo
import com.wanderwildwood.okiba.core.History
import com.wanderwildwood.okiba.core.Spot
import com.wanderwildwood.okiba.core.cleanNote
import com.wanderwildwood.okiba.data.Store
import com.wanderwildwood.okiba.device.FixResult
import com.wanderwildwood.okiba.device.Locator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** Why the last press did not give a result, in the words the screen will use. */
enum class Notice { NoFix, LocationOff, NeedPrecise }

/** How far the person is from the shown spot, as of one fresh fix. */
data class Far(
    val spotTime: Long,
    val meters: Double,
    val compass: Compass,
    val accuracyMeters: Float,
    val atTheSpot: Boolean,
)

enum class Job { Mark, Far }

data class UiState(
    val history: History = History(),
    val working: Job? = null,
    val notice: Notice? = null,
    val far: Far? = null,
    /** Wall-clock now, refreshed whenever the app comes to the front; the screen never ticks. */
    val now: Long = System.currentTimeMillis(),
)

class OkibaViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)
    private val locator = Locator(app)
    private val _state = MutableStateFlow(UiState(history = store.load()))
    val state: StateFlow<UiState> = _state

    val hasPermission: Boolean get() = locator.hasPermission()

    fun refreshClock() = _state.update { it.copy(now = System.currentTimeMillis()) }

    /** One fresh fix for [job]; the permission has been dealt with by the caller. */
    fun start(job: Job) {
        if (_state.value.working != null) return
        _state.update { it.copy(working = job, notice = null) }
        locator.request { r -> onFix(job, r) }
    }

    fun stop() {
        locator.cancel()
        _state.update { it.copy(working = null) }
    }

    fun denied() = _state.update { it.copy(notice = Notice.NeedPrecise) }

    fun dismissNotice() = _state.update { it.copy(notice = null) }

    private fun onFix(job: Job, r: FixResult) {
        when (r) {
            is FixResult.Fix -> when (job) {
                Job.Mark -> {
                    val h = _state.value.history.marked(Spot(r.latitude, r.longitude, r.accuracyMeters, r.timeMillis))
                    store.save(h)
                    _state.update { it.copy(history = h, working = null, far = null, now = r.timeMillis) }
                }
                Job.Far -> {
                    val spot = _state.value.history.shown
                    if (spot == null) {
                        _state.update { it.copy(working = null) }
                    } else {
                        val d = Geo.distanceMeters(r.latitude, r.longitude, spot.latitude, spot.longitude)
                        val b = Geo.bearingDegrees(r.latitude, r.longitude, spot.latitude, spot.longitude)
                        val far = Far(
                            spot.timeMillis, d, Geo.compass(b), r.accuracyMeters,
                            Geo.isWithinMargin(d, r.accuracyMeters.toDouble(), spot.accuracyMeters.toDouble()),
                        )
                        _state.update { it.copy(working = null, far = far, now = r.timeMillis) }
                    }
                }
            }
            FixResult.NotAllowed -> _state.update { it.copy(working = null, notice = Notice.NeedPrecise) }
            FixResult.Off -> _state.update { it.copy(working = null, notice = Notice.LocationOff) }
            FixResult.NoFix -> _state.update { it.copy(working = null, notice = Notice.NoFix) }
        }
    }

    fun show(spot: Spot) = change { it.show(spot) }

    fun forget(spot: Spot) = change { it.forgotten(spot) }

    fun setNote(spot: Spot, note: String) = change(clearFar = false) { it.withNote(spot, note.cleanNote()) }

    private fun change(clearFar: Boolean = true, f: (History) -> History) {
        val h = f(_state.value.history)
        store.save(h)
        _state.update { it.copy(history = h, far = if (clearFar) null else it.far) }
    }

    override fun onCleared() {
        locator.cancel()
    }
}
