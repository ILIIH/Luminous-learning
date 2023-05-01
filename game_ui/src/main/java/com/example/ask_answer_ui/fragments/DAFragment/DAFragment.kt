package com.example.ask_answer_ui.fragments.DAFragment

import android.icu.text.SimpleDateFormat
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.add_new_card_data.model.LearningCardDomain
import com.example.ask_answer_ui.adapter.AnswerAdapter
import com.example.ask_answer_ui.databinding.FragmentDABinding
import com.example.ask_answer_ui.viewModel.DA_ViewModel
import com.example.ask_answer_ui.viewModel.cardProvider
import org.koin.android.ext.android.inject
import java.util.*

class DAFragment : Fragment() {

    val viewModel: DA_ViewModel by inject()
    val cardProvider: cardProvider by inject()
    lateinit var answerAdapter: AnswerAdapter
    var isFragmentClosed = false

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = FragmentDABinding.inflate(inflater, container, false)
        val currentCard = cardProvider._currentCard as LearningCardDomain
        val begin = System.nanoTime()

        answerAdapter = AnswerAdapter {
            if (it) {
                cardProvider.updateLearningCardInfoAndMetrics(
                    currentDate = Date(),
                    cardDateCreation = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse(
                        currentCard.dateCreation,
                    ),
                    AverageRA = currentCard.AverageRA,
                    result = true,
                    Time = begin - System.nanoTime(),
                    card = currentCard
                )
            } else {
                cardProvider.updateLearningCardInfoAndMetrics(
                    currentDate = Date(),
                    cardDateCreation = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse(
                        currentCard.dateCreation,
                    ),
                    AverageRA = currentCard.AverageRA,
                    result = false,
                    Time = begin - System.nanoTime(),
                    card = currentCard
                )
            }
            isFragmentClosed = true
            goToNextCard(view)
        }

        DescriptionDialog(currentCard.discription).show(parentFragmentManager, "description_dialog")

        view.answerList.adapter = answerAdapter

        view.answerList.isNestedScrollingEnabled = false

        Log.i("card_logging", "Question : " + currentCard.question)
        view.question.text = currentCard.question

        val endTime = 10000L
        view.timeView.setEndingTime(endTime.toFloat())

        val timer = object : CountDownTimer(endTime, 100) {
            override fun onTick(millisUntilFinished: Long) {
                view.timeView.setCurTime(millisUntilFinished.toFloat())
            }

            override fun onFinish() {
                if (!isFragmentClosed) {
                    DescriptionDialog("Time is ended").show(
                        parentFragmentManager,
                        "description_dialog",
                    )
                    cardProvider.updateLearningCardInfoAndMetrics(
                        currentDate = Date(),
                        cardDateCreation = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse(
                            currentCard.dateCreation,
                        ),
                        AverageRA = currentCard.AverageRA,
                        result = false,
                        Time = begin - System.nanoTime(),
                        card = currentCard
                    )
                    goToNextCard(view)
                }
            }
        }

        answerAdapter.submitList(currentCard.answers)

        timer.start()

        return view.root
    }

    fun goToNextCard(view: FragmentDABinding) {
        cardProvider.goToNextCard()
        if (!cardProvider.hasDACard()) {
            Handler().postDelayed({
                lifecycleScope.launchWhenResumed {
                    findNavController().popBackStack()
                }
            }, 1000)
        } else {
            val currentCard = cardProvider._currentCard as LearningCardDomain
            answerAdapter.submitList(currentCard.answers)
            view.question.text = currentCard.question
            DescriptionDialog("Description").show(parentFragmentManager, "description_dialog")
        }
    }
}
