package com.example.iot.feature.control.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

abstract class BaseControlFragment<VB : ViewBinding> : Fragment() {

    private var _binding: VB? = null
    protected val b: VB get() = _binding!!

    protected abstract fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): VB
    protected abstract val vm: BaseControlViewModel

    protected val remoteId: String by lazy { requireArguments().getString("remoteId").orEmpty() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = inflateBinding(inflater, container)
        return b.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    protected fun bindToolbar(toolbar: MaterialToolbar) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.title.collect { toolbar.subtitle = it }
            }
        }
    }
}
