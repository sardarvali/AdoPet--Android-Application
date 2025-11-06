package com.syed.activities

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.*
import androidx.appcompat.app.AppCompatActivity
import com.syed.MainActivity
import com.syed.databinding.ActivitySplashBinding
import com.syed.utils.FirebaseUtils
import kotlin.math.cos
import kotlin.math.sin

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    private val splashDuration = 4000L // 4 seconds for more animations

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide status bar for immersive experience
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        initializeViews()
        startAnimations()
        navigateToMainScreen()
    }

    private fun initializeViews() {
        // Initial setup - make elements invisible
        binding.apply {
            // Logo and main elements
            logoContainer.alpha = 0f
            logoContainer.scaleX = 0.3f
            logoContainer.scaleY = 0.3f

            appName.alpha = 0f
            appName.translationY = 80f

            taglineContainer.alpha = 0f
            taglineContainer.translationY = 60f

            // Clouds
            cloud1.alpha = 0f
            cloud1.translationX = 200f
            cloud2.alpha = 0f
            cloud2.translationX = -200f

            // Floating pets
            floatingDog.alpha = 0f
            floatingDog.scaleX = 0f
            floatingDog.scaleY = 0f

            floatingCat.alpha = 0f
            floatingCat.scaleX = 0f
            floatingCat.scaleY = 0f

            floatingBird.alpha = 0f
            floatingBird.scaleX = 0f
            floatingBird.scaleY = 0f

            // Paw trail
            pawTrail1.alpha = 0f
            pawTrail1.scaleX = 0f
            pawTrail1.scaleY = 0f

            pawTrail2.alpha = 0f
            pawTrail2.scaleX = 0f
            pawTrail2.scaleY = 0f

            pawTrail3.alpha = 0f
            pawTrail3.scaleX = 0f
            pawTrail3.scaleY = 0f

            pawTrail4.alpha = 0f
            pawTrail4.scaleX = 0f
            pawTrail4.scaleY = 0f

            // Decorative paw prints
            pawPrint1.alpha = 0f
            pawPrint1.scaleX = 0f
            pawPrint1.scaleY = 0f

            pawPrint2.alpha = 0f
            pawPrint2.scaleX = 0f
            pawPrint2.scaleY = 0f

            pawPrint3.alpha = 0f
            pawPrint3.scaleX = 0f
            pawPrint3.scaleY = 0f

            // Orbiting pets
            orbitingPet1.alpha = 0f
            orbitingPet2.alpha = 0f
            orbitingPet3.alpha = 0f

            // Bottom pets
            bottomPetsContainer.alpha = 0f
            bottomPetsContainer.translationY = 100f

            heart.alpha = 0f
            heart.scaleX = 0f
            heart.scaleY = 0f
        }
    }

    private fun startAnimations() {
        // Phase 1: Clouds floating in (0-800ms)
        startCloudAnimations()

        // Phase 2: Logo entrance (400-1200ms)
        Handler(Looper.getMainLooper()).postDelayed({ startLogoAnimation() }, 400)

        // Phase 3: Floating pets appear (800-1600ms)
        Handler(Looper.getMainLooper()).postDelayed({ startFloatingPetAnimations() }, 800)

        // Phase 4: Paw trail sequence (1200-2400ms)
        Handler(Looper.getMainLooper()).postDelayed({ startPawTrailAnimation() }, 1200)

        // Phase 5: Text animations (1600-2400ms)
        Handler(Looper.getMainLooper()).postDelayed({ startTextAnimations() }, 1600)

        // Phase 6: Orbiting pets (2000-2800ms)
        Handler(Looper.getMainLooper()).postDelayed({ startOrbitingPetAnimations() }, 2000)

        // Phase 7: Bottom pets bouncing in (2400-3200ms)
        Handler(Looper.getMainLooper()).postDelayed({ startBottomPetAnimations() }, 2400)

        // Phase 8: Decorative paw prints (2800-3600ms)
        Handler(Looper.getMainLooper()).postDelayed({ startDecorativePawAnimations() }, 2800)
    }

    private fun startCloudAnimations() {
        // Cloud 1 - floating from right
        val cloud1FadeIn =
            ObjectAnimator.ofFloat(binding.cloud1, "alpha", 0f, 0.3f).apply {
                duration = 800
            }
        val cloud1SlideIn =
            ObjectAnimator.ofFloat(binding.cloud1, "translationX", 200f, 0f).apply {
                duration = 800
                interpolator = DecelerateInterpolator()
            }

        // Cloud 2 - floating from left
        val cloud2FadeIn =
            ObjectAnimator.ofFloat(binding.cloud2, "alpha", 0f, 0.25f).apply {
                duration = 800
                startDelay = 200
            }
        val cloud2SlideIn =
            ObjectAnimator.ofFloat(binding.cloud2, "translationX", -200f, 0f).apply {
                duration = 800
                startDelay = 200
                interpolator = DecelerateInterpolator()
            }

        AnimatorSet().apply {
            playTogether(cloud1FadeIn, cloud1SlideIn, cloud2FadeIn, cloud2SlideIn)
            start()
        }

        // Start continuous floating motion for clouds
        startContinuousCloudFloating()
    }

    private fun startContinuousCloudFloating() {
        // Cloud 1 continuous floating
        val cloud1Float =
            ObjectAnimator.ofFloat(binding.cloud1, "translationY", 0f, -20f, 0f).apply {
                duration = 3000
                repeatCount = ObjectAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
            }

        // Cloud 2 continuous floating (different rhythm)
        val cloud2Float =
            ObjectAnimator.ofFloat(binding.cloud2, "translationY", 0f, 15f, 0f).apply {
                duration = 4000
                repeatCount = ObjectAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
                startDelay = 1500
            }

        cloud1Float.start()
        cloud2Float.start()
    }

    private fun startLogoAnimation() {
        val logoFadeIn =
            ObjectAnimator.ofFloat(binding.logoContainer, "alpha", 0f, 1f).apply {
                duration = 800
                interpolator = AccelerateDecelerateInterpolator()
            }

        val logoScaleX =
            ObjectAnimator.ofFloat(binding.logoContainer, "scaleX", 0.3f, 1.2f, 1f).apply {
                duration = 800
                interpolator = OvershootInterpolator()
            }

        val logoScaleY =
            ObjectAnimator.ofFloat(binding.logoContainer, "scaleY", 0.3f, 1.2f, 1f).apply {
                duration = 800
                interpolator = OvershootInterpolator()
            }

        AnimatorSet().apply {
            playTogether(logoFadeIn, logoScaleX, logoScaleY)
            start()
        }

        // Start logo gentle breathing animation
        Handler(Looper.getMainLooper()).postDelayed({
            startLogoBreathing()
        }, 800)
    }

    private fun startLogoBreathing() {
        val breathingScale =
            ObjectAnimator.ofFloat(binding.logoImage, "scaleX", 1f, 1.05f, 1f).apply {
                duration = 2000
                repeatCount = ObjectAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
            }
        val breathingScaleY =
            ObjectAnimator.ofFloat(binding.logoImage, "scaleY", 1f, 1.05f, 1f).apply {
                duration = 2000
                repeatCount = ObjectAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
            }

        AnimatorSet().apply {
            playTogether(breathingScale, breathingScaleY)
            start()
        }
    }

    private fun startFloatingPetAnimations() {
        // Floating dog with bouncing entrance
        createFloatingPetAnimation(binding.floatingDog, 0L)

        // Floating cat with delayed bouncing entrance
        createFloatingPetAnimation(binding.floatingCat, 300L)

        // Floating bird with delayed bouncing entrance
        createFloatingPetAnimation(binding.floatingBird, 600L)
    }

    private fun createFloatingPetAnimation(
        view: View,
        delay: Long,
    ) {
        val fadeIn =
            ObjectAnimator.ofFloat(view, "alpha", 0f, 0.4f).apply {
                duration = 600
                startDelay = delay
            }

        val scaleX =
            ObjectAnimator.ofFloat(view, "scaleX", 0f, 1f).apply {
                duration = 600
                startDelay = delay
                interpolator = BounceInterpolator()
            }

        val scaleY =
            ObjectAnimator.ofFloat(view, "scaleY", 0f, 1f).apply {
                duration = 600
                startDelay = delay
                interpolator = BounceInterpolator()
            }

        AnimatorSet().apply {
            playTogether(fadeIn, scaleX, scaleY)
            start()
        }

        // Start continuous floating motion
        Handler(Looper.getMainLooper()).postDelayed({
            startPetFloatingMotion(view)
        }, delay + 600)
    }

    private fun startPetFloatingMotion(view: View) {
        val floatY =
            ObjectAnimator.ofFloat(view, "translationY", 0f, -30f, 0f).apply {
                duration = 2500 + (Math.random() * 1000).toLong() // Random duration for natural feel
                repeatCount = ObjectAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
            }

        val floatX =
            ObjectAnimator.ofFloat(view, "translationX", 0f, 20f, 0f).apply {
                duration = 3000 + (Math.random() * 1500).toLong()
                repeatCount = ObjectAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
            }

        floatY.start()
        floatX.start()
    }

    private fun startPawTrailAnimation() {
        // Sequential paw trail animation
        createPawTrailStep(binding.pawTrail1, 0L)
        createPawTrailStep(binding.pawTrail2, 200L)
        createPawTrailStep(binding.pawTrail3, 400L)
        createPawTrailStep(binding.pawTrail4, 600L)
    }

    private fun createPawTrailStep(
        view: View,
        delay: Long,
    ) {
        val fadeIn =
            ObjectAnimator.ofFloat(view, "alpha", 0f, 0.6f).apply {
                duration = 400
                startDelay = delay
            }

        val scaleX =
            ObjectAnimator.ofFloat(view, "scaleX", 0f, 1f).apply {
                duration = 400
                startDelay = delay
                interpolator = OvershootInterpolator()
            }

        val scaleY =
            ObjectAnimator.ofFloat(view, "scaleY", 0f, 1f).apply {
                duration = 400
                startDelay = delay
                interpolator = OvershootInterpolator()
            }

        AnimatorSet().apply {
            playTogether(fadeIn, scaleX, scaleY)
            start()
        }
    }

    private fun startTextAnimations() {
        // App name slide up with bounce
        val appNameFadeIn =
            ObjectAnimator.ofFloat(binding.appName, "alpha", 0f, 1f).apply {
                duration = 800
            }

        val appNameSlideUp =
            ObjectAnimator.ofFloat(binding.appName, "translationY", 80f, 0f).apply {
                duration = 800
                interpolator = OvershootInterpolator()
            }

        // Tagline slide up with delay
        val taglineFadeIn =
            ObjectAnimator.ofFloat(binding.taglineContainer, "alpha", 0f, 1f).apply {
                duration = 600
                startDelay = 400
            }

        val taglineSlideUp =
            ObjectAnimator.ofFloat(binding.taglineContainer, "translationY", 60f, 0f).apply {
                duration = 600
                startDelay = 400
                interpolator = OvershootInterpolator()
            }

        AnimatorSet().apply {
            playTogether(appNameFadeIn, appNameSlideUp, taglineFadeIn, taglineSlideUp)
            start()
        }

        // Start heart animation after tagline appears
        Handler(Looper.getMainLooper()).postDelayed({
            startHeartAnimation()
        }, 800)
    }

    private fun startHeartAnimation() {
        val heartFadeIn =
            ObjectAnimator.ofFloat(binding.heart, "alpha", 0f, 1f).apply {
                duration = 400
            }

        val heartScale =
            ObjectAnimator.ofFloat(binding.heart, "scaleX", 0f, 1f).apply {
                duration = 400
                interpolator = OvershootInterpolator()
            }

        val heartScaleY =
            ObjectAnimator.ofFloat(binding.heart, "scaleY", 0f, 1f).apply {
                duration = 400
                interpolator = OvershootInterpolator()
            }

        AnimatorSet().apply {
            playTogether(heartFadeIn, heartScale, heartScaleY)
            start()
        }

        // Start continuous heartbeat
        Handler(Looper.getMainLooper()).postDelayed({
            startHeartbeat()
        }, 400)
    }

    private fun startHeartbeat() {
        val heartbeatX =
            ObjectAnimator.ofFloat(binding.heart, "scaleX", 1f, 1.3f, 1f).apply {
                duration = 600
                repeatCount = ObjectAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
            }

        val heartbeatY =
            ObjectAnimator.ofFloat(binding.heart, "scaleY", 1f, 1.3f, 1f).apply {
                duration = 600
                repeatCount = ObjectAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
            }

        AnimatorSet().apply {
            playTogether(heartbeatX, heartbeatY)
            start()
        }
    }

    private fun startOrbitingPetAnimations() {
        // Fade in orbiting pets
        val orbitFadeIn1 =
            ObjectAnimator.ofFloat(binding.orbitingPet1, "alpha", 0f, 1f).apply {
                duration = 500
            }
        val orbitFadeIn2 =
            ObjectAnimator.ofFloat(binding.orbitingPet2, "alpha", 0f, 1f).apply {
                duration = 500
                startDelay = 200
            }
        val orbitFadeIn3 =
            ObjectAnimator.ofFloat(binding.orbitingPet3, "alpha", 0f, 1f).apply {
                duration = 500
                startDelay = 400
            }

        AnimatorSet().apply {
            playTogether(orbitFadeIn1, orbitFadeIn2, orbitFadeIn3)
            start()
        }

        // Start orbital motion
        Handler(Looper.getMainLooper()).postDelayed({
            startOrbitalMotion()
        }, 600)
    }

    private fun startOrbitalMotion() {
        val radius = 100f
        val centerX = binding.logoContainer.x + binding.logoContainer.width / 2
        val centerY = binding.logoContainer.y + binding.logoContainer.height / 2

        // Orbiting pet 1
        createOrbitalAnimation(binding.orbitingPet1, centerX, centerY, radius, 0f, 4000L)

        // Orbiting pet 2
        createOrbitalAnimation(binding.orbitingPet2, centerX, centerY, radius, 120f, 4000L)

        // Orbiting pet 3
        createOrbitalAnimation(binding.orbitingPet3, centerX, centerY, radius, 240f, 4000L)
    }

    private fun createOrbitalAnimation(
        view: View,
        centerX: Float,
        centerY: Float,
        radius: Float,
        startAngle: Float,
        duration: Long,
    ) {
        val animator =
            ValueAnimator.ofFloat(0f, 360f).apply {
                this.duration = duration
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()

                addUpdateListener { animation ->
                    val angle = Math.toRadians((animation.animatedValue as Float + startAngle).toDouble())
                    val x = centerX + radius * cos(angle).toFloat() - view.width / 2
                    val y = centerY + radius * sin(angle).toFloat() - view.height / 2

                    view.x = x
                    view.y = y
                }
            }
        animator.start()
    }

    private fun startBottomPetAnimations() {
        // Bottom pets container slide up
        val containerFadeIn =
            ObjectAnimator.ofFloat(binding.bottomPetsContainer, "alpha", 0f, 1f).apply {
                duration = 800
            }

        val containerSlideUp =
            ObjectAnimator.ofFloat(binding.bottomPetsContainer, "translationY", 100f, 0f).apply {
                duration = 800
                interpolator = OvershootInterpolator()
            }

        AnimatorSet().apply {
            playTogether(containerFadeIn, containerSlideUp)
            start()
        }

        // Start individual pet bouncing
        Handler(Looper.getMainLooper()).postDelayed({
            startBottomPetBouncing()
        }, 800)
    }

    private fun startBottomPetBouncing() {
        // Create bouncing animations for each bottom pet with different delays
        createBottomPetBounce(binding.bottomPet1, 0L)
        createBottomPetBounce(binding.bottomPet2, 200L)
        createBottomPetBounce(binding.bottomPet3, 400L)
        createBottomPetBounce(binding.bottomPet4, 600L)
    }

    private fun createBottomPetBounce(
        view: View,
        delay: Long,
    ) {
        Handler(Looper.getMainLooper()).postDelayed({
            val bounce =
                ObjectAnimator.ofFloat(view, "translationY", 0f, -20f, 0f).apply {
                    duration = 800
                    repeatCount = ObjectAnimator.INFINITE
                    interpolator = BounceInterpolator()
                    repeatMode = ObjectAnimator.RESTART
                }
            bounce.start()
        }, delay)
    }

    private fun startDecorativePawAnimations() {
        // Decorative paw prints with staggered bounce
        createDecorativePawAnimation(binding.pawPrint1, 0L)
        createDecorativePawAnimation(binding.pawPrint2, 300L)
        createDecorativePawAnimation(binding.pawPrint3, 600L)
    }

    private fun createDecorativePawAnimation(
        view: View,
        delay: Long,
    ) {
        val fadeIn =
            ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
                duration = 500
                startDelay = delay
            }

        val scaleX =
            ObjectAnimator.ofFloat(view, "scaleX", 0f, 1f).apply {
                duration = 500
                startDelay = delay
                interpolator = BounceInterpolator()
            }

        val scaleY =
            ObjectAnimator.ofFloat(view, "scaleY", 0f, 1f).apply {
                duration = 500
                startDelay = delay
                interpolator = BounceInterpolator()
            }

        AnimatorSet().apply {
            playTogether(fadeIn, scaleX, scaleY)
            start()
        }
    }

    private fun navigateToMainScreen() {
        Handler(Looper.getMainLooper()).postDelayed({
            val intent =
                if (FirebaseUtils.auth.currentUser != null) {
                    Intent(this, MainActivity::class.java)
                } else {
                    Intent(this, LoginActivity::class.java)
                }
            startActivity(intent)
            finish()

            // Add transition animation
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, splashDuration)
    }
}
