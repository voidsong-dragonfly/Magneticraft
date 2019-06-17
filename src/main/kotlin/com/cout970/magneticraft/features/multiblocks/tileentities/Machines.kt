package com.cout970.magneticraft.features.multiblocks.tileentities

import com.cout970.magneticraft.api.internal.energy.ElectricNode
import com.cout970.magneticraft.api.internal.heat.HeatNode
import com.cout970.magneticraft.features.multiblocks.structures.*
import com.cout970.magneticraft.misc.ElectricConstants
import com.cout970.magneticraft.misc.RegisterTileEntity
import com.cout970.magneticraft.misc.crafting.GrinderCraftingProcess
import com.cout970.magneticraft.misc.crafting.HydraulicPressCraftingProcess
import com.cout970.magneticraft.misc.crafting.SieveCraftingProcess
import com.cout970.magneticraft.misc.fluid.Tank
import com.cout970.magneticraft.misc.fromCelsiusToKelvin
import com.cout970.magneticraft.misc.inventory.Inventory
import com.cout970.magneticraft.misc.inventory.InventoryCapabilityFilter
import com.cout970.magneticraft.misc.tileentity.DoNotRemove
import com.cout970.magneticraft.misc.vector.createAABBUsing
import com.cout970.magneticraft.misc.vector.rotateBox
import com.cout970.magneticraft.misc.vector.rotatePoint
import com.cout970.magneticraft.misc.vector.vec3Of
import com.cout970.magneticraft.misc.world.ParticleSpawner
import com.cout970.magneticraft.misc.world.isServer
import com.cout970.magneticraft.registry.ELECTRIC_NODE_HANDLER
import com.cout970.magneticraft.registry.FLUID_HANDLER
import com.cout970.magneticraft.registry.HEAT_NODE_HANDLER
import com.cout970.magneticraft.registry.ITEM_HANDLER
import com.cout970.magneticraft.systems.config.Config
import com.cout970.magneticraft.systems.multiblocks.Multiblock
import com.cout970.magneticraft.systems.tilemodules.*
import com.cout970.magneticraft.systems.tilerenderers.px
import com.cout970.vector.extensions.Vector3
import net.minecraft.init.Blocks
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumParticleTypes
import net.minecraft.util.ITickable
import net.minecraft.util.math.BlockPos

@RegisterTileEntity("grinder")
class TileGrinder : TileMultiblock(), ITickable {

    override fun getMultiblock(): Multiblock = MultiblockGrinder

    val node = ElectricNode(ref)

    val inventory = Inventory(3)

    val itemExporterModule = ModuleItemExporter(
        facing = { facing },
        inventory = InventoryCapabilityFilter(inventory, listOf(1, 2), listOf(1, 2)),
        ports = { listOf(BlockPos(0, 0, -3) to EnumFacing.SOUTH, BlockPos(0, 0, -3) to EnumFacing.UP) }
    )

    val openGuiModule = ModuleOpenGui()

    val ioModule: ModuleMultiblockIO = ModuleMultiblockIO(
        facing = { facing },
        connectionSpots = listOf(
            ConnectionSpot(
                capability = ELECTRIC_NODE_HANDLER!!,
                pos = BlockPos(1, 1, -1),
                side = EnumFacing.EAST,
                getter = { energyModule }
            ),
            ConnectionSpot(ITEM_HANDLER!!, BlockPos(0, 0, -2), EnumFacing.NORTH,
                getter = { InventoryCapabilityFilter(inventory, emptyList(), listOf(1, 2)) }
            ),
            ConnectionSpot(ITEM_HANDLER!!, BlockPos(-1, 0, -1), EnumFacing.WEST,
                getter = { InventoryCapabilityFilter(inventory, listOf(0), emptyList()) }
            )
        ) + ModuleMultiblockIO.connectionArea(ITEM_HANDLER!!, BlockPos(-1, 3, -2), BlockPos(1, 3, 0),
            EnumFacing.UP, getter = { InventoryCapabilityFilter(inventory, listOf(0), emptyList()) }
        )
    )

    val energyModule = ModuleElectricity(
        electricNodes = listOf(node),
        canConnectAtSide = ioModule::canConnectAtSide,
        connectableDirections = ioModule::getElectricConnectPoints
    )

    val storageModule = ModuleInternalStorage(
        mainNode = node,
        capacity = 10_000,
        lowerVoltageLimit = ElectricConstants.TIER_1_MACHINES_MIN_VOLTAGE,
        upperVoltageLimit = ElectricConstants.TIER_1_MACHINES_MIN_VOLTAGE
    )

    val invModule = ModuleInventory(
        inventory = inventory,
        capabilityFilter = ModuleInventory.ALLOW_NONE
    )

    val processModule = ModuleElectricProcessing(
        craftingProcess = GrinderCraftingProcess(invModule, 0, 1, 2),
        storage = storageModule,
        workingRate = 1f,
        costPerTick = Config.grinderMaxConsumption.toFloat()
    )

    override val multiblockModule = ModuleMultiblockCenter(
        multiblockStructure = getMultiblock(),
        facingGetter = { facing },
        capabilityGetter = ioModule::getCapability
    )

    val spawner = ParticleSpawner(30.0, EnumParticleTypes.BLOCK_DUST, Blocks.STONE.defaultState,
        speed = { facing.rotatePoint(Vector3.ORIGIN, vec3Of(0, 0.1, 0)) }
    ) {
        facing.rotateBox(vec3Of(0.5), vec3Of((-5).px, 14.px, 5.px).createAABBUsing(vec3Of(21.px, 58.px, (-21).px))).offset(pos)
    }

    init {
        initModules(multiblockModule, energyModule, storageModule, processModule, invModule, openGuiModule, ioModule,
            itemExporterModule)
    }

    @DoNotRemove
    override fun update() {
        super.update()

        if (world.isServer || !processModule.working) return
        spawner.spawn(world)
    }
}

@RegisterTileEntity("sieve")
class TileSieve : TileMultiblock(), ITickable {

    override fun getMultiblock(): Multiblock = MultiblockSieve

    val node = ElectricNode(ref)
    val inventory = Inventory(4)

    val itemExporterModule0 = ModuleItemExporter(
        facing = { facing },
        inventory = InventoryCapabilityFilter(inventory, listOf(1), listOf(1)),
        ports = { listOf(BlockPos(0, -1, -1) to EnumFacing.UP) }
    )

    val itemExporterModule1 = ModuleItemExporter(
        facing = { facing },
        inventory = InventoryCapabilityFilter(inventory, listOf(2), listOf(2)),
        ports = { listOf(BlockPos(0, -1, -2) to EnumFacing.UP) }
    )

    val itemExporterModule2 = ModuleItemExporter(
        facing = { facing },
        inventory = InventoryCapabilityFilter(inventory, listOf(3), listOf(3)),
        ports = { listOf(BlockPos(0, -1, -3) to EnumFacing.UP) }
    )

    val openGuiModule = ModuleOpenGui()

    val ioModule: ModuleMultiblockIO = ModuleMultiblockIO(
        facing = { facing },
        connectionSpots = listOf(
            ConnectionSpot(
                capability = ELECTRIC_NODE_HANDLER!!,
                pos = BlockPos(-1, 1, 0),
                side = EnumFacing.SOUTH,
                getter = { energyModule }
            ),
            ConnectionSpot(
                capability = ELECTRIC_NODE_HANDLER!!,
                pos = BlockPos(1, 1, 0),
                side = EnumFacing.SOUTH,
                getter = { energyModule }
            ),
            ConnectionSpot(ITEM_HANDLER!!, BlockPos(0, 1, 0), EnumFacing.UP,
                getter = { InventoryCapabilityFilter(inventory, listOf(0), emptyList()) }
            ),
            ConnectionSpot(ITEM_HANDLER!!, BlockPos(0, 0, 0), EnumFacing.SOUTH,
                getter = { InventoryCapabilityFilter(inventory, listOf(0), emptyList()) }
            ),
            ConnectionSpot(ITEM_HANDLER!!, BlockPos(0, 0, -1), EnumFacing.DOWN,
                getter = { InventoryCapabilityFilter(inventory, emptyList(), listOf(1)) }
            ),
            ConnectionSpot(ITEM_HANDLER!!, BlockPos(0, 0, -2), EnumFacing.DOWN,
                getter = { InventoryCapabilityFilter(inventory, emptyList(), listOf(2)) }
            ),
            ConnectionSpot(ITEM_HANDLER!!, BlockPos(0, 0, -3), EnumFacing.DOWN,
                getter = { InventoryCapabilityFilter(inventory, emptyList(), listOf(3)) }
            )
        )
    )

    val energyModule = ModuleElectricity(
        electricNodes = listOf(node),
        canConnectAtSide = ioModule::canConnectAtSide,
        connectableDirections = ioModule::getElectricConnectPoints
    )

    val storageModule = ModuleInternalStorage(
        mainNode = node,
        capacity = 10_000,
        lowerVoltageLimit = ElectricConstants.TIER_1_MACHINES_MIN_VOLTAGE,
        upperVoltageLimit = ElectricConstants.TIER_1_MACHINES_MIN_VOLTAGE
    )

    val invModule = ModuleInventory(
        inventory = inventory,
        capabilityFilter = ModuleInventory.ALLOW_NONE
    )

    val processModule = ModuleElectricProcessing(
        craftingProcess = SieveCraftingProcess(invModule, 0, 1, 2, 3),
        storage = storageModule,
        workingRate = 1f,
        costPerTick = Config.sieveMaxConsumption.toFloat()
    )

    override val multiblockModule = ModuleMultiblockCenter(
        multiblockStructure = getMultiblock(),
        facingGetter = { facing },
        capabilityGetter = ioModule::getCapability
    )

    val spawner1 = ParticleSpawner(20.0, EnumParticleTypes.BLOCK_DUST, Blocks.SAND.defaultState,
        speed = { facing.rotatePoint(Vector3.ORIGIN, vec3Of(0, 0, -0.1)) }
    ) {
        facing.rotateBox(vec3Of(0.5), vec3Of((-4).px, 1.5, (-4).px).createAABBUsing(vec3Of(20.px, 1.5, (-20).px))).offset(pos)
    }
    val spawner2 = ParticleSpawner(20.0, EnumParticleTypes.BLOCK_DUST, Blocks.SAND.defaultState,
        speed = { facing.rotatePoint(Vector3.ORIGIN, vec3Of(0, 0, -0.1)) }
    ) {
        facing.rotateBox(vec3Of(0.5), vec3Of((-4).px, 1.25, (-20).px).createAABBUsing(vec3Of(20.px, 1.25, (-36).px))).offset(pos)
    }
    val spawner3 = ParticleSpawner(20.0, EnumParticleTypes.BLOCK_DUST, Blocks.SAND.defaultState,
        speed = { facing.rotatePoint(Vector3.ORIGIN, vec3Of(0, 0, -0.1)) }
    ) {
        facing.rotateBox(vec3Of(0.5), vec3Of((-4).px, 1, (-36).px).createAABBUsing(vec3Of(20.px, 1, (-52).px))).offset(pos)
    }

    init {
        initModules(multiblockModule, energyModule, storageModule, processModule, invModule, ioModule, openGuiModule,
            itemExporterModule0, itemExporterModule1, itemExporterModule2)
    }

    @DoNotRemove
    override fun update() {
        super.update()

        if (world.isServer || !processModule.working) return
        spawner1.spawn(world)
        spawner2.spawn(world)
        spawner3.spawn(world)
    }
}

@RegisterTileEntity("hydraulic_press")
class TileHydraulicPress : TileMultiblock(), ITickable {

    override fun getMultiblock(): Multiblock = MultiblockHydraulicPress

    val inventory = Inventory(2)
    val node = ElectricNode(ref)

    val invModule = ModuleInventory(inventory, capabilityFilter = ModuleInventory.ALLOW_NONE)

    val storageModule = ModuleInternalStorage(
        mainNode = node,
        capacity = 10_000,
        lowerVoltageLimit = ElectricConstants.TIER_1_MACHINES_MIN_VOLTAGE,
        upperVoltageLimit = ElectricConstants.TIER_1_MACHINES_MIN_VOLTAGE
    )

    val openGuiModule = ModuleOpenGui()

    val ioModule: ModuleMultiblockIO = ModuleMultiblockIO(
        facing = { facing },
        connectionSpots = listOf(ConnectionSpot(
            capability = ELECTRIC_NODE_HANDLER!!,
            pos = BlockPos(-1, 1, -1),
            side = EnumFacing.WEST,
            getter = { if (active) energyModule else null }
        ), ConnectionSpot(
            capability = ELECTRIC_NODE_HANDLER!!,
            pos = BlockPos(1, 1, -1),
            side = EnumFacing.EAST,
            getter = { if (active) energyModule else null }
        ), ConnectionSpot(
            capability = ITEM_HANDLER!!,
            pos = BlockPos(0, 0, 0),
            side = EnumFacing.SOUTH,
            getter = { if (active) InventoryCapabilityFilter(inventory, listOf(0), listOf()) else null }
        ), ConnectionSpot(
            capability = ITEM_HANDLER!!,
            pos = BlockPos(0, 0, -2),
            side = EnumFacing.NORTH,
            getter = { if (active) InventoryCapabilityFilter(inventory, listOf(), listOf(1)) else null }
        ))
    )

    val energyModule = ModuleElectricity(
        electricNodes = listOf(node),
        canConnectAtSide = ioModule::canConnectAtSide,
        connectableDirections = ioModule::getElectricConnectPoints
    )

    val hydraulicPressModule = ModuleHydraulicPress()

    val processModule = ModuleElectricProcessing(
        costPerTick = Config.hydraulicPressMaxConsumption.toFloat(),
        workingRate = 1f,
        storage = storageModule,
        craftingProcess = HydraulicPressCraftingProcess(
            inventory = inventory,
            inputSlot = 0,
            outputSlot = 1,
            mode = hydraulicPressModule::mode
        )
    )

    val itemExporterModule = ModuleItemExporter(
        facing = { facing },
        inventory = InventoryCapabilityFilter(inventory, listOf(1), listOf(1)),
        ports = { listOf(BlockPos(0, 0, -3) to EnumFacing.SOUTH, BlockPos(0, 0, -3) to EnumFacing.UP) }
    )

    override val multiblockModule = ModuleMultiblockCenter(
        multiblockStructure = getMultiblock(),
        facingGetter = { facing },
        capabilityGetter = ioModule::getCapability
    )

    init {
        initModules(multiblockModule, invModule, energyModule, storageModule, ioModule,
            openGuiModule, processModule, hydraulicPressModule, itemExporterModule)
    }

    @DoNotRemove
    override fun update() {
        super.update()
    }
}

@RegisterTileEntity("big_combustion_chamber")
class TileBigCombustionChamber : TileMultiblock(), ITickable {

    override fun getMultiblock(): Multiblock = MultiblockBigCombustionChamber

    val inventory = Inventory(1)
    val node = HeatNode(ref)
    val tank = Tank(4000)

    val openGuiModule = ModuleOpenGui()
    val fluidModule = ModuleFluidHandler(tank)
    val invModule = ModuleInventory(inventory)

    val ioModule: ModuleMultiblockIO = ModuleMultiblockIO(
        facing = { facing },
        connectionSpots = listOf(ConnectionSpot(
            capability = ITEM_HANDLER!!,
            pos = BlockPos(-1, 0, -1),
            side = EnumFacing.WEST,
            getter = { if (active) inventory else null }
        ), ConnectionSpot(
            capability = ITEM_HANDLER!!,
            pos = BlockPos(1, 0, -1),
            side = EnumFacing.EAST,
            getter = { if (active) inventory else null }
        ), ConnectionSpot(
            capability = FLUID_HANDLER!!,
            pos = BlockPos(-1, 0, -1),
            side = EnumFacing.WEST,
            getter = { if (active) tank else null }
        ), ConnectionSpot(
            capability = FLUID_HANDLER!!,
            pos = BlockPos(1, 0, -1),
            side = EnumFacing.EAST,
            getter = { if (active) tank else null }
        )) + ModuleMultiblockIO.connectionArea(HEAT_NODE_HANDLER!!,
            BlockPos(-1, 1, -2), BlockPos(1, 1, 0), EnumFacing.UP
        ) { if (active) heatModule else null }
    )

    val heatModule = ModuleHeat(listOf(node), connectableDirections = ioModule::getHeatConnectPoints)
    val bigCombustionChamberModule = ModuleBigCombustionChamber(this::facing, node, inventory, tank, 2000.fromCelsiusToKelvin())

    override val multiblockModule = ModuleMultiblockCenter(
        multiblockStructure = getMultiblock(),
        facingGetter = this::facing,
        capabilityGetter = ioModule::getCapability
    )

    init {
        initModules(multiblockModule, invModule, fluidModule, bigCombustionChamberModule, heatModule, openGuiModule)
    }

    @DoNotRemove
    override fun update() {
        super.update()
    }
}

@RegisterTileEntity("big_steam_boiler")
class TileBigSteamEngine : TileMultiblock(), ITickable {

    override fun getMultiblock(): Multiblock = MultiblockBigSteamBoiler

    val ioModule: ModuleMultiblockIO = ModuleMultiblockIO(
        facing = { facing },
        connectionSpots = emptyList()
    )

    override val multiblockModule = ModuleMultiblockCenter(
        multiblockStructure = getMultiblock(),
        facingGetter = this::facing,
        capabilityGetter = ioModule::getCapability
    )

    @DoNotRemove
    override fun update() {
        super.update()
    }
}