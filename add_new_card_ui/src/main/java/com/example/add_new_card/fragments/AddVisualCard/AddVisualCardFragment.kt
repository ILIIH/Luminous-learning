package com.example.add_new_card.fragments.AddVisualCard

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isEmpty
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.add_new_card.R
import com.example.add_new_card.adapters.AnswersAdapters
import com.example.add_new_card.databinding.FragmentAddAudioCardBinding
import com.example.add_new_card.databinding.FragmentAddVisualCardBinding
import com.example.add_new_card.fragments.RuleFragment.ThemeInfoProvider
import com.example.add_new_card.util.hideKeyboard
import com.example.core.domain.ILError
import com.example.core.ui.BaseFragment
import org.koin.android.ext.android.inject
import java.util.*

class AddVisualCardFragment : BaseFragment() {

    val viewModel: AddVisualCardViewmodel by inject()
    val mainViewModel: ThemeInfoProvider by inject()

    val adapter = AnswersAdapters()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view: FragmentAddVisualCardBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_add_visual_card, container, false)

        view.questionInputText.addTextChangedListener {
            view.question.error = null
        }

        view.answers.adapter = adapter
        adapter.submitList(viewModel.getAnswers())

        view.addNewAnswer.setOnClickListener {
            viewModel.addAnswer()
            adapter.submitList(viewModel.getAnswers())
            adapter.notifyItemInserted(viewModel.getAnswers().size)
        }

        val themeId = mainViewModel.getThemeId()

        view.addPhotoLayout.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            launcher.launch(intent)
        }

        view.continueBtn.setOnClickListener {
            val answers = adapter.getAllAnswers()
            AlertDialog.Builder(context)
                .setTitle("Creation card")
                .setMessage("Do you want to continue creation or add this card and exit?")
                .setPositiveButton(
                    getString(R.string.continue_creation),
                ) { _, _ ->
                    viewModel.addNewCard(
                        themeId = themeId,
                        question = view.question.editText!!.text.toString(),
                        answers,
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(Date()),
                        monthNumber = Date().month,
                    )
                    if(!adapter.validateAnswers() && !validateCard(view)){
                        view.Title.requestFocus()
                        view.addPhoto.setBackgroundResource(R.drawable.baseline_image_search_24)
                        view.questionInputText.text?.clear()
                        initEmptyAnswers()
                    }
                }
                .setNegativeButton(
                    R.string.save_and_exit,
                ) { _, _ ->
                    viewModel.addNewCard(
                        themeId = themeId,
                        question = view.question.editText!!.text.toString(),
                        answers,
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(Date()),
                        monthNumber = Date().month,
                    )
                    hideKeyboard(requireActivity())
                    findNavController().popBackStack()
                }
                .setIcon(R.drawable.baseline_credit_card_24)
                .show()
        }

        viewModel._photo.observe(requireActivity()) {
            it?.let { photo ->
                view.addPhoto.setImageDrawable(null)
                view.addPhoto.setImageBitmap(photo)
            }
        }
        view.lifecycleOwner = viewLifecycleOwner
        return view.root
    }

    private val launcher: ActivityResultLauncher<Intent> =
        registerForActivityResult<Intent, ActivityResult>(
            ActivityResultContracts.StartActivityForResult(),
        ) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK &&
                result.data != null
            ) {
                val photoUri: Uri = result.data!!.data!!
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(
                            requireContext().contentResolver,
                            photoUri,
                        ),
                    )
                } else {
                    MediaStore.Images.Media.getBitmap(requireContext().contentResolver, photoUri)
                }
                viewModel.setPhoto(getResizedBitmap(bitmap, 1000)!!)
            }
        }
    fun validateCard(view: FragmentAddVisualCardBinding): Boolean {
        var result = false
        if(view.questionInputText.text.toString().isEmpty()){
            view.question.error = getString(R.string.enter_question)
            view.question.requestFocus()
            result = true
        }

        if(viewModel._photo.value != null){
            showError(ILError.VALIDATION_PHOTO)
            result = true
        }
        return result
    }
    fun getResizedBitmap(image: Bitmap, maxSize: Int): Bitmap? {
        var width: Int = image.getWidth()
        var height: Int = image.getHeight()
        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1) {
            width = maxSize
            height = (width / bitmapRatio).toInt()
        } else {
            height = maxSize
            width = (height * bitmapRatio).toInt()
        }
        return Bitmap.createScaledBitmap(image, width, height, true)
    }

    private fun initEmptyAnswers() {
        val size = viewModel.getAnswers().size-1
        viewModel.reInitAnswers()
        adapter.submitList(viewModel.getAnswers())
        adapter.notifyItemRangeRemoved(1, size)
        adapter.notifyItemChanged(0)
    }
}
