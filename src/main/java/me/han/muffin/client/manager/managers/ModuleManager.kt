package me.han.muffin.client.manager.managers

import me.han.muffin.client.module.Module
import me.han.muffin.client.module.modules.combat.*
import me.han.muffin.client.module.modules.exploits.*
import me.han.muffin.client.module.modules.hidden.ChestSwapModule
import me.han.muffin.client.module.modules.hidden.FakePlayerModule
import me.han.muffin.client.module.modules.hidden.FreecamDupeModule
import me.han.muffin.client.module.modules.hidden.Pull32k
import me.han.muffin.client.module.modules.misc.*
import me.han.muffin.client.module.modules.movement.*
import me.han.muffin.client.module.modules.other.*
import me.han.muffin.client.module.modules.player.*
import me.han.muffin.client.module.modules.render.*
import org.lwjgl.input.Keyboard
import kotlin.concurrent.thread

object ModuleManager {
    @JvmField val modules = ArrayList<Module>()

    init {
        addMod(AutoWalkModule)
        addMod(EntitySpeedModule)
        addMod(BedAuraModule)
        addMod(FreecamModule)
        addMod(ChamsRewriteModule)
        addMod(HudEditorModule)
        addMod(NoHandAnimationModule)
        addMod(HoleAnchorModule)
        addMod(TriggerModule)
        addMod(MurdererDetector())
        addMod(ItemRender)
        addMod(AntiBotModule)
        addMod(SafeWalkModule)
        addMod(ReverseStepModule)
        addMod(FastDropModule)
        addMod(SpeedModule)
        addMod(LongJumpModule)
        addMod(Aura())
        addMod(CriticalsModule)
        addMod(AntiFeetMineModule)
        addMod(AutoRespawnModule)

        addMod(DirectionTest)

        //CHAT
        addMod(KillAnnouncerModule)
        addMod(ChatTweaksModule)
        addMod(BoatFlyModule)

        //COMBAT
        addMod(Auto32kModule2)
        addMod(AutoFeetPlace)
        addMod(AnnouncerModule)
        addMod(FastBowModule)
        addMod(BowAimModule)
        addMod(OffhandCrystalModule)
        addMod(SelfShootModule)
        addMod(AutoTotemModule)
        addMod(AutoTrapModule)
        addMod(FastPlaceModule)
        addMod(HoleFillerModule)
        addMod(HopperBreakerModule)
        addMod(AutoCrystalModule)
        addMod(StrictTotemModule)
        addMod(CrystalPistonModule)

        //EXPLOITS
        addMod(NewChunksModule)
        addMod(ChestBackpackModule)
        addMod(CoordExploitModule)
        addMod(EntityDesyncModule)
        addMod(NoMineAnimationModule)
        addMod(ReachModule)
        //addMod(QueueSkipperModule)
        addMod(GodModeModule)
        addMod(PhaseModule)
        addMod(CrasherModule)
        //    addMod(PacketFlyModule.INSTANCE);
        addMod(PacketFlyModule)
        addMod(XCarryModule)
        // do here because we implements xcarry in autoarmour
        addMod(AutoArmourModule)
        addMod(AutoHoleMineModule)
        addMod(CrystalBlocksModule)
        addMod(PingSpoofModule)
        addMod(AutoSelfTrapModule)
        addMod(InvalidTeleportModule())

        //HIDDEN
        addMod(Pull32k)
        addMod(FreecamDupeModule)
        addMod(ChestSwapModule)

        //MISC
        addMod(AutoMountModule)
        addMod(AntiAimModule)
        addMod(Debugger)
        addMod(ScaffoldModule)
        addMod(AutoLogModule)
        addMod(AutoReconnectModule)
        addMod(CameraClipModule)
        addMod(PacketOptimizerModule)
        addMod(FPSLimitModule)
        addMod(DiscordPresence())
        addMod(LogoutSpotModule)
        addMod(MiddleClickModule)
        addMod(TimerModule)
        addMod(NotificationsModule)
        addMod(Nuker())
        addMod(BetterPortalsModule)
        addMod(FakePlayerModule)
        addMod(AntiVanishModule)
        addMod(PayloadSpoofModule)
        addMod(SpammerModule)

        // GUI
        addMod(AvoidModule)
        addMod(FovModule)
        addMod(ClickGUI)
        addMod(ColorControl())
        addMod(MainMenuModule)
        addMod(FontsModule)
        addMod(StreamerModeModule)
        addMod(CombatStatusModule)
        addMod(NoHandshakeModule)
        addMod(RenderModeModule)

        //MOVEMENT
        addMod(JesusModule)
        addMod(AntiEffectsModule)
        addMod(NoSlowModule)
        addMod(SprintModule)
        addMod(ElytraFlyModule)
        addMod(HighJumpModule)
        addMod(StepModule)
        addMod(LiquidSpeedModule)
        addMod(FakeLagModule)
        addMod(EntityControlModule)
        addMod(FlightModule)
        addMod(NoFallModule)
        addMod(VelocityModule)

        //PLAYER
        addMod(AntiHungerModule)
        addMod(AutoReplenish())
        addMod(InteractionTweaksModule)
        addMod(NoRotateModule)
        addMod(SpeedMineModule)
        addMod(AutoFeetPlaceModule)
        addMod(OffhandGapModule)
        addMod(WebModule)
        addMod(NoGlitchBlocksModule)
        addMod(RotationLockModule)
        addMod(ElytraFly2b2tModule)

        //RENDER
        addMod(CustomEnchantModule)
        addMod(BreakEspModule)
        addMod(AnimationModule)
        addMod(AntiOverlayModule)
        addMod(BlockHighlightModule)
        addMod(FillEspModule)
        addMod(ChamsModule)
        addMod(EntityESPModule)
        addMod(CrosshairModule)
        addMod(StorageEspModule)
        addMod(HitMarkersModule)
        addMod(BetterTabModule)
        addMod(FullBrightModule)
        addMod(HoleEspModule)
        addMod(NoRenderModule)
        addMod(TooltipModule)
        addMod(VoidEspModule)
        addMod(TracersModule)
        addMod(TrajectoriesModule)
        addMod(WallHackModule)
        addMod(SearchModule)
        addMod(NametagsModule)
        addMod(SelfFillModule)
        addMod(PacketLoggerModule)
        addMod(AutoCrystalHelper)
        addMod(AutoDupeModule)
        addMod(HudModule)

        addMod(FeetEspModule)

        modules.forEach { if (it.isEnabled) it.enable() }
        initSortedList()
    }

    private fun initSortedList() {
        thread { modules.sortedBy { it.name } }
    }

    private fun addMod(m: Module) {
        modules.add(m)
    }

    @JvmStatic
    fun onBind(key: Int) {
        if (key == 0 || Keyboard.isKeyDown(Keyboard.KEY_F3)) return
        modules.forEach { if (it.bind == key) it.toggle() }
    }

    fun getModule(name: String): Module? {
        return modules.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }

    fun getModule(clazz: Class<out Module?>): Module? {
        return modules.firstOrNull { it.javaClass == clazz }
    }

    @Deprecated("")
    fun isModuleEnabled(moduleName: String): Boolean {
        val module = getModule(moduleName) ?: return false
        return module.isEnabled
    }

    fun isModuleEnabled(clazz: Class<out Module?>): Boolean {
        val module = getModule(clazz) ?: return false
        return module.isEnabled
    }

}