package club.sk1er.patcher.render

import club.sk1er.elementa.components.*
import club.sk1er.elementa.constraints.CenterConstraint
import club.sk1er.elementa.constraints.ChildBasedSizeConstraint
import club.sk1er.elementa.constraints.SiblingConstraint
import club.sk1er.elementa.constraints.animation.Animations
import club.sk1er.elementa.dsl.*
import club.sk1er.mods.core.universal.UResolution
import club.sk1er.patcher.screen.ScreenHistory
import club.sk1er.patcher.util.name.NameFetcher
import club.sk1er.vigilance.gui.VigilancePalette
import net.minecraft.client.Minecraft
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.modcore.api.ModCoreAPI
import net.modcore.api.utils.Multithreading
import org.lwjgl.input.Mouse
import java.net.URL
import java.util.concurrent.ConcurrentLinkedQueue

object HistoryPopUp {
    private val window = Window()
    private val fetchers = ConcurrentLinkedQueue<NameFetcher>()

    init {
        UIContainer() childOf window
    }

    @SubscribeEvent
    fun render(event: RenderGameOverlayEvent.Post) {
        if (event.type == RenderGameOverlayEvent.ElementType.TEXT && Minecraft.getMinecraft().currentScreen !is ScreenHistory) {
            window.draw()
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase == TickEvent.Phase.END) {
            val x = fetchers.poll() ?: return
            PopUp(x) childOf window
        }
    }

    @SubscribeEvent
    fun onClick(event: GuiScreenEvent.MouseInputEvent.Post) {
        val mouseX = Mouse.getEventX() * event.gui.width / UResolution.windowWidth
        val mouseY = event.gui.height - Mouse.getEventY() * event.gui.height / UResolution.windowHeight - 1
        val eventButton = Mouse.getEventButton()
        if (Mouse.getEventButtonState()) {
            window.mouseClick(mouseX.toDouble(), mouseY.toDouble(), eventButton)
        } else if (eventButton != -1) {
            window.mouseRelease()
        }
    }

    fun addPopUp(player: String) {
        Multithreading.runAsync(Runnable {
            val nf = NameFetcher()
            nf.execute(player, false)
            fetchers.add(nf)
        })
    }

    private class PopUp(val fetcher: NameFetcher) : UIBlock(VigilancePalette.DARK_BACKGROUND) {
        val img = UIImage.ofURL(URL("https://cravatar.eu/helmavatar/${fetcher.uuid.toString().replace("-", "")}")).constrain {
            x = 5.percent()
            y = CenterConstraint()
            width = 25.percent()
            height = basicHeightConstraint { it.getWidth() }
        }
        val timerBar = UIBlock(VigilancePalette.ACCENT).constrain {
            x = 0.pixels()
            y = 0.pixels(true)
            height = 2.pixels()
            width = 0.percent()
        }

        init {
            if (fetcher.uuid != null) {
                constrain {
                    x = 10.pixels(true)
                    y = SiblingConstraint(10f)
                    height = 80.pixels()
                    width = 200.pixels()
                }

                //this effect OutlineEffect(VigilancePalette.ACCENT, 2f)

                // todo when new-infra modcore branch is merged, cache this image.
                img childOf this

                onMouseClick {
                    // todo add thing that shows what each mouse button does
                    when (it.mouseButton) {
                        0 -> {
                            ModCoreAPI.getGuiUtil().openScreen(ScreenHistory(fetcher.name))
                            window.removeChild(this@PopUp)
                        }
                        1 -> {
                            // todo animate this
                            println(this@PopUp.getHeight())
                            window.removeChild(this@PopUp)
                        }
                        else -> window.removeChild(this@PopUp)
                    }
                }

                timerBar childOf this
            }

            timer(5000L) {
                window.removeChild(this)
            }
        }

        override fun afterInitialization() {
            val t = UIText(fetcher.name, false).constrain {
                x = 35.percent()
                y = basicYConstraint { img.getTop() }
                textScale = 1.1f.pixels()
                color = VigilancePalette.BRIGHT_TEXT.toConstraint()
            } childOf this

            var j = 9 * 1.1f + 2

            fun mkText(str: String, i: Int): UIText = UIText(str, false).constrain {
                x = 35.percent()
                y = SiblingConstraint(if (i == 0) 3f else 1f)
                textScale = .8f.pixels()
                color = when (i) {
                    0 -> VigilancePalette.BRIGHT_TEXT
                    1 -> VigilancePalette.MID_TEXT
                    else -> VigilancePalette.DARK_TEXT
                }.toConstraint()//VigilancePalette.BRIGHT_TEXT.toConstraint()
            }

            fetcher.names.reverse()
            for (i in fetcher.names.indices) {
                if (i < 5) {
                    mkText(fetcher.names[i], i) childOf this
                    j += 9.1f
                } else {
                    mkText("...", 4) childOf this
                    break
                }
            }

            timerBar.animate {
                setWidthAnimation(Animations.LINEAR, 5f, 100.percent())
            }

            super.afterInitialization()
        }
    }
}