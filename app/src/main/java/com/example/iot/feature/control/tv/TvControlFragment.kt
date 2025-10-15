package com.example.iot.feature.control.tv

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.example.iot.databinding.FragmentControlTvBinding
import com.example.iot.feature.control.common.BaseControlFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TvControlFragment :
    BaseControlFragment<FragmentControlTvBinding>() {

    override val vm: TvControlViewModel by viewModels()

    override fun inflateContent(
        inflater: LayoutInflater,
        container: ViewGroup
    ): FragmentControlTvBinding =
        FragmentControlTvBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm.load(remoteId)

        // Hàng trên cùng
        b.btnSource.setOnClickListener { vm.tvAv() }
        b.btnMute.setOnClickListener { vm.mute() }
        b.btnPower.setOnClickListener { vm.togglePower() } // nếu bạn có btnPower

        // Cụm VOL / CH
        b.btnPlus.setOnClickListener { vm.volUp() }
        b.btnMinus.setOnClickListener { vm.volDown() }
        b.btnChUp.setOnClickListener { vm.chUp() }
        b.btnChDown.setOnClickListener { vm.chDown() }

        // Menu – Exit
        b.btnMenu.setOnClickListener { vm.menu() }
        b.btnExit.setOnClickListener { vm.exit() }

        // Link hàng chữ
        b.link123.setOnClickListener { showDigits(true) }
        b.linkMenu.setOnClickListener { showDigits(false) }
        b.linkMore.setOnClickListener { /* nếu cần thêm màn khác */ }

        // D-pad
        b.btnUp.setOnClickListener { vm.navUp() }
        b.btnDown.setOnClickListener { vm.navDown() }
        b.btnLeft.setOnClickListener { vm.navLeft() }
        b.btnRight.setOnClickListener { vm.navRight() }
        b.btnOk.setOnClickListener { vm.ok() }

        // Cụm số
        b.txt0.setOnClickListener { vm.digit(0) }
        b.txt1.setOnClickListener { vm.digit(1) }
        b.txt2.setOnClickListener { vm.digit(2) }
        b.txt3.setOnClickListener { vm.digit(3) }
        b.txt4.setOnClickListener { vm.digit(4) }
        b.txt5.setOnClickListener { vm.digit(5) }
        b.txt6.setOnClickListener { vm.digit(6) }
        b.txt7.setOnClickListener { vm.digit(7) }
        b.txt8.setOnClickListener { vm.digit(8) }
        b.txt9.setOnClickListener { vm.digit(9) }
    }

    /** BaseControlFragment sẽ gọi khi bấm thùng rác trên toolbar */
    override fun onConfirmDelete() {
        vm.delete(remoteId)
    }

    /* Hiển thị/ẩn lưới số tuỳ theo layout bạn đang dùng */
    private fun showDigits(show: Boolean) {
        // nếu layout có container cho lưới số, bật/tắt ở đây
        b.gridDigits?.visibility = if (show) View.VISIBLE else View.GONE
        b.rowLinks?.visibility  = if (show) View.GONE   else View.VISIBLE
    }
}
