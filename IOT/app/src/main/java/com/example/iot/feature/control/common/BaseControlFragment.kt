package com.example.iot.feature.control.common

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import com.example.iot.R
import com.example.iot.databinding.FragmentControlBaseBinding
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import androidx.core.view.size
import androidx.core.view.get

/**
 * Base cho các màn điều khiển (AC/TV/…).
 * Cung cấp sẵn: toolbar (back + delete) và banner online/offline.
 * Child chỉ cần implement inflateContent() để trả về ViewBinding của nội dung.
 */
abstract class BaseControlFragment<VB : ViewBinding> : Fragment() {

    private var _baseBinding: FragmentControlBaseBinding? = null
    protected val baseBinding get() = _baseBinding!!

    private var _contentBinding: VB? = null
    protected val b get() = _contentBinding!!

    /** ViewModel phải kế thừa BaseControlViewModel (có title & isNodeOnline). */
    protected abstract val vm: BaseControlViewModel

    /** Child cung cấp binding cho phần nội dung (attachToParent = false). */
    protected abstract fun inflateContent(
        inflater: LayoutInflater,
        container: ViewGroup
    ): VB

    /** Child override để thực hiện xóa điều khiển. */
    protected open fun onConfirmDelete() {}

    /** remoteId truyền qua nav-args. */
    protected val remoteId: String by lazy {
        requireArguments().getString("remoteId").orEmpty()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _baseBinding = FragmentControlBaseBinding.inflate(inflater, container, false)

        // Inflate content an toàn (KHÔNG ép kiểu mù)
        val contentParent: FrameLayout = baseBinding.contentContainer
        _contentBinding = inflateContent(inflater, contentParent)
        contentParent.addView(b.root)

        setupToolbar(baseBinding.topBarControl)
        observeCommonUi()

        return baseBinding.root
    }

    private fun setupToolbar(tb: MaterialToolbar) {
        tb.setNavigationOnClickListener { findNavController().navigateUp() }
        tb.navigationIcon?.let { DrawableCompat.setTint(DrawableCompat.wrap(it), Color.WHITE) }
        for (i in 0 until tb.menu.size) {
            tb.menu[i].icon?.let { DrawableCompat.setTint(DrawableCompat.wrap(it), Color.WHITE) }
        }

        tb.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_delete -> {
                    AlertDialog.Builder(requireContext())
                        .setTitle(R.string.delete_remote)
                        .setMessage(getString(R.string.delete_confirm))
                        .setPositiveButton(R.string.delete_remote) { _, _ ->
                            onConfirmDelete()
                            findNavController().popBackStack(R.id.homeFragment, false)
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                    true
                }
                else -> false
            }
        }
    }

    private fun observeCommonUi() {
        // Banner online/offline
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.isNodeOnline.collect { online ->
                    baseBinding.bannerOffline.visibility = if (online) View.GONE else View.VISIBLE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.title.collect { title ->
                    baseBinding.topBarControl.title = title
                    baseBinding.topBarControl.subtitle = null
                }
            }
        }
    }

    override fun onDestroyView() {
        baseBinding.topBarControl.menu.clear()
        baseBinding.topBarControl.visibility = View.GONE

        (baseBinding.root.parent as? ViewGroup)?.removeView(baseBinding.root)

        _contentBinding = null
        _baseBinding = null
        super.onDestroyView()
    }
}