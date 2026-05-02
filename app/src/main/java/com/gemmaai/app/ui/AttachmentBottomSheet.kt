package com.gemmaai.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gemmaai.app.R
import com.gemmaai.app.databinding.BottomSheetAttachmentBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AttachmentBottomSheet(
    private val onOptionSelected: (AttachmentOption) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAttachmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAttachmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.optionImage.setOnClickListener {
            onOptionSelected(AttachmentOption.IMAGE)
            dismiss()
        }

        binding.optionFile.setOnClickListener {
            onOptionSelected(AttachmentOption.FILE)
            dismiss()
        }
    }

    override fun getTheme() = R.style.Theme_GemmaAI_BottomSheet

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
