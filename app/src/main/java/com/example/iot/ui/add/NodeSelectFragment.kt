package com.example.iot.ui.add

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.iot.R
import com.example.iot.databinding.FragmentSimpleListBinding
import com.example.iot.ui.common.SimpleStringAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NodeSelectFragment : Fragment() {
    private var _b: FragmentSimpleListBinding? = null
    private val b get() = _b!!
    private val vm: NodeSelectViewModel by viewModels()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentSimpleListBinding.inflate(i, c, false)   // layout đơn giản: RecyclerView + topBar
        return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)
        val type = requireArguments().getString("type") ?: ""
        val brand = requireArguments().getString("brand") ?: ""

        val toolbar: androidx.appcompat.widget.Toolbar = b.topBar.root

        toolbar.title = getString(R.string.app_name)
        toolbar.subtitle = "Chọn thiết bị hồng ngoại"
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        val adapter = SimpleStringAdapter { nodeId ->
            findNavController().navigate(
                R.id.action_nodeSelect_to_codeSetTest,
                Bundle().apply {
                    putString("type", type)
                    putString("brand", brand)
                    putString("nodeId", nodeId)
                }
            )
        }
        b.recycler.layoutManager = LinearLayoutManager(requireContext())
        b.recycler.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.nodes.collectLatest { adapter.submit(it) }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
