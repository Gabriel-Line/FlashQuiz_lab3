package com.example.flashquiz

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import android.view.ViewAnimationUtils
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.animation.AnimationUtils
import android.view.animation.Animation

class MainActivity : AppCompatActivity() {

    private lateinit var cardQuestion: TextView
    private lateinit var cardAnswer: TextView
    private lateinit var db: AppDatabase
    private var flashcardList: List<Flashcard> = listOf()
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cardQuestion = findViewById(R.id.card_question)
        cardAnswer = findViewById(R.id.card_answer)
        val addCardButton = findViewById<ImageView>(R.id.btn_add_card)
        val btnNext = findViewById<ImageView>(R.id.btn_next) // Icône flèche '>'

        // Initialisation de la base de données Room
        db = AppDatabase.getDatabase(applicationContext)

        // Chargement des cartes depuis la base de données au démarrage
        loadFlashcardsFromDatabase()

        // Lancement pour récupérer le résultat de AddCardActivity
        val addCardLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                // Recharger les cartes et aller sur la dernière ajoutée
                loadFlashcardsFromDatabase()
            }
        }

        // 1. Cliquer sur la question révèle la réponse
        cardQuestion.setOnClickListener {
            // S'assurer que cardAnswer est prêt à être révélé
            val cx = cardAnswer.width / 2
            val cy = cardAnswer.height / 2
            val finalRadius = Math.hypot(cx.toDouble(), cy.toDouble()).toFloat()

            // On rend la vue visible juste avant de lancer l'animation circulaire
            cardAnswer.visibility = View.VISIBLE

            val anim = ViewAnimationUtils.createCircularReveal(cardAnswer, cx, cy, 0f, finalRadius)
            anim.duration = 300
            anim.start()

            // Cacher la question
            cardQuestion.visibility = View.INVISIBLE
        }

        // 2. Cliquer sur la réponse réaffiche la question
        cardAnswer.setOnClickListener {
            val cx = cardAnswer.width / 2
            val cy = cardAnswer.height / 2
            val initialRadius = Math.hypot(cx.toDouble(), cy.toDouble()).toFloat()

            val anim = ViewAnimationUtils.createCircularReveal(cardAnswer, cx, cy, initialRadius, 0f)
            anim.duration = 300

            anim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    cardAnswer.visibility = View.INVISIBLE
                    cardQuestion.visibility = View.VISIBLE
                }
            })

            anim.start()
        }

        // Bouton "+" pour ajouter une carte avec animation
        addCardButton.setOnClickListener {
            val intent = Intent(this, AddCardActivity::class.java)
            addCardLauncher.launch(intent)

            // Application de l'animation de transition
            overridePendingTransition(R.anim.right_in, R.anim.left_out)
        }

        // Icône '>' pour passer à la carte suivante avec animation de glissement
        btnNext.setOnClickListener {
            if (flashcardList.isNotEmpty()) {
                // 1. Charger l'animation de sortie (glisse vers la gauche)
                val leftOutAnim = AnimationUtils.loadAnimation(this, R.anim.left_out)

                // 2. Appliquer un Animation Listener
                leftOutAnim.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {}

                    override fun onAnimationEnd(animation: Animation?) {
                        // Mettre à jour l'index et afficher la nouvelle carte
                        currentIndex = (currentIndex + 1) % flashcardList.size
                        displayCard(currentIndex)

                        // 3. Charger et lancer l'animation d'entrée pour la nouvelle carte (glisse depuis la droite)
                        val rightInAnim = AnimationUtils.loadAnimation(this@MainActivity, R.anim.right_in)
                        cardQuestion.startAnimation(rightInAnim)
                    }

                    override fun onAnimationRepeat(animation: Animation?) {}
                })

                // Démarrer l'animation de sortie sur la question actuelle
                cardQuestion.startAnimation(leftOutAnim)
            }
        }
    }

    // Charger toutes les cartes de la base de données en arrière-plan
    private fun loadFlashcardsFromDatabase() {
        Thread {
            flashcardList = db.flashcardDao().getAllCards()

            runOnUiThread {
                if (flashcardList.isNotEmpty()) {
                    currentIndex = flashcardList.size - 1 // Affiche la plus récente par défaut
                    displayCard(currentIndex)
                } else {
                    cardQuestion.text = "Aucune carte disponible. Cliquez sur + pour en ajouter !"
                    cardAnswer.text = ""
                }
            }
        }.start()
    }

    // Afficher une carte spécifique selon son index
    private fun displayCard(index: Int) {
        val card = flashcardList[index]
        cardQuestion.text = card.question
        cardAnswer.text = card.answer

        // S'assurer que la question est visible et la réponse cachée au changement de carte
        cardQuestion.visibility = View.VISIBLE
        cardAnswer.visibility = View.INVISIBLE
    }
}