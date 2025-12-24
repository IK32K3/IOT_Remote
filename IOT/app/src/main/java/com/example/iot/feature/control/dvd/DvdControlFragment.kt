package com.example.iot.feature.control.dvd

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.iot.databinding.FragmentControlDvdBinding
import com.example.iot.feature.control.common.BaseControlFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DvdControlFragment : BaseControlFragment<FragmentControlDvdBinding>() {

    override val vm: DvdControlViewModel by viewModels()

    override fun inflateContent(
        inflater: LayoutInflater,
        container: ViewGroup
    ) = FragmentControlDvdBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm.load(remoteId)
        vm.showBasic()

        // Top row
        b.btnPower.setOnClickListener { vm.togglePower() }
        b.btnMute.setOnClickListener { vm.mute() }
        b.btnEject.setOnClickListener { vm.eject() }

        // Volume
        b.btnVolUp.setOnClickListener { vm.volUp() }
        b.btnVolDown.setOnClickListener { vm.volDown() }

        // Playback
        b.btnRew.setOnClickListener { vm.rew() }
        b.btnFf.setOnClickListener { vm.ff() }
        b.btnPlayPause.setOnClickListener { vm.playPause() }
        b.btnStop.setOnClickListener { vm.stop() }
        b.btnPrev.setOnClickListener { vm.prev() }
        b.btnNext.setOnClickListener { vm.next() }

        // Links
        b.link123.setOnClickListener {
            if (b.gridDigits.isVisible) vm.showBasic() else vm.showDigits()
        }
        b.linkMenu.setOnClickListener { vm.showBasic(); vm.menu() }
        b.linkMore.setOnClickListener {
            if (b.gridMore.isVisible) vm.showBasic() else vm.showMore()
        }

        // Floating around dpad
        b.btnMenuFloat.setOnClickListener { vm.menu() }
        b.btnExitFloat.setOnClickListener { vm.exit() }
        b.btnMenuBottom.setOnClickListener { vm.menu() }
        b.btnMoreBottom.setOnClickListener { vm.home() }

        // D-pad
        b.btnOk.setOnClickListener { vm.ok() }
        b.btnUp.setOnClickListener { vm.up() }
        b.btnDown.setOnClickListener { vm.down() }
        b.btnLeft.setOnClickListener { vm.left() }
        b.btnRight.setOnClickListener { vm.right() }

        // Digits
        b.gridDigits.apply {
            getChildAt(0).setOnClickListener { vm.digit(1) }
            getChildAt(1).setOnClickListener { vm.digit(2) }
            getChildAt(2).setOnClickListener { vm.digit(3) }
            getChildAt(3).setOnClickListener { vm.digit(4) }
            getChildAt(4).setOnClickListener { vm.digit(5) }
            getChildAt(5).setOnClickListener { vm.digit(6) }
            getChildAt(6).setOnClickListener { vm.digit(7) }
            getChildAt(7).setOnClickListener { vm.digit(8) }
            getChildAt(8).setOnClickListener { vm.digit(9) }
            getChildAt(9).setOnClickListener { vm.dash() }
            getChildAt(10).setOnClickListener { vm.digit(0) }
            getChildAt(11).setOnClickListener { vm.back(); vm.showBasic() }
        }

        // More grid
        b.gridMore.apply {
            getChildAt(0).setOnClickListener { vm.title() }
            getChildAt(1).setOnClickListener { vm.subtitle() }
            getChildAt(2).setOnClickListener { vm.red() }
            getChildAt(3).setOnClickListener { vm.yellow() }
            getChildAt(4).setOnClickListener { vm.green() }
            getChildAt(5).setOnClickListener { vm.blue() }
        }

        // Page observer
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.page.collect { p ->
                    val isBasic = p == DvdPage.BASIC
                    val isDigits = p == DvdPage.DIGITS
                    val isMore = p == DvdPage.MORE

                    b.rowTop.isVisible = true
                    b.pillVol.isVisible = true
                    b.colPlaybackLeft.isVisible = true
                    b.colPlaybackRight.isVisible = true

                    b.dpad.isVisible = isBasic
                    b.btnMenuFloat.isVisible = isBasic
                    b.btnExitFloat.isVisible = isBasic
                    b.btnMenuBottom.isVisible = isBasic
                    b.btnMoreBottom.isVisible = isBasic

                    b.gridDigits.isVisible = isDigits
                    b.gridMore.isVisible = isMore

                    b.rowLinks.isVisible = true
                }
            }
        }
    }

    override fun onConfirmDelete(remoteId: String) = vm.deleteRemote(remoteId)
}
