package dev.aaur1s.minecraft.fluidcells.client

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonObject
import com.mojang.datafixers.util.Pair
import dev.aaur1s.minecraft.fluidcells.api.item.FluidCellItemBase
import dev.aaur1s.minecraft.fluidcells.client.FluidCellModel.Substance.Type.*
import dev.aaur1s.minecraft.fluidcells.util.*
import mekanism.common.registries.MekanismGases
import mekanism.common.registries.MekanismInfuseTypes
import mekanism.common.registries.MekanismPigments
import mekanism.common.registries.MekanismSlurries
import net.minecraft.client.renderer.model.*
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.LivingEntity
import net.minecraft.item.ItemStack
import net.minecraft.resources.IResourceManager
import net.minecraft.util.Direction
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.ForgeHooksClient
import net.minecraftforge.client.model.*
import net.minecraftforge.client.model.geometry.IModelGeometry
import net.minecraftforge.fml.ModList
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.resource.IResourceType
import net.minecraftforge.resource.VanillaResourceType
import java.util.function.Function
import java.util.function.Predicate


// minimal Z offset to prevent depth-fighting
private const val NORTH_Z_COVER = 7.496f / 16f
private const val SOUTH_Z_COVER = 8.504f / 16f
private const val NORTH_Z_FLUID = 7.498f / 16f
private const val SOUTH_Z_FLUID = 8.502f / 16f

private const val REBAKE_LOCATION = "fluidcells:substance_container_override"

data class FluidCellModel(
    val substance: Substance
) : IModelGeometry<FluidCellModel> {

    override fun bake(
        owner: IModelConfiguration,
        bakery: ModelBakery,
        spriteGetter: Function<RenderMaterial, TextureAtlasSprite?>,
        modelTransform: IModelTransform,
        overrides: ItemOverrideList,
        modelLocation: ResourceLocation
    ): IBakedModel {
        val particleLocation = owner.resolveTextureOrNull("particle")
        val baseLocation = owner.resolveTextureOrNull("base")
        val maskLocation = owner.resolveTextureOrNull("mask")
        val coverLocation = owner.resolveTextureOrNull("cover")

        var luminosity = 0
        var color = 0

        val substanceSprite = when (substance.type) {
            FLUID if FLUID.runtimeRequiredModsIsLoaded() -> {
                val fluid = ForgeRegistries.FLUIDS.entries.first { it.key.location().toString() == substance.id }.value
                luminosity = fluid.attributes.luminosity
                color = fluid.attributes.color
                spriteGetter.apply(ForgeHooksClient.getBlockMaterial(fluid.attributes.stillTexture))
            }
            GAS if GAS.runtimeRequiredModsIsLoaded() -> {
                val gas = MekanismGases.GASES.forgeRegistry.entries.first { it.id.toString() == substance.id }.get()
                color = gas.colorRepresentation
                gas.textureAtlasSprite
            }
            INFUSE if INFUSE.runtimeRequiredModsIsLoaded() -> {
                val infuse = MekanismInfuseTypes.INFUSE_TYPES.forgeRegistry.entries.first { it.id.toString() == substance.id }.get()
                color = infuse.colorRepresentation
                infuse.textureAtlasSprite
            }
            PIGMENT if PIGMENT.runtimeRequiredModsIsLoaded() -> {
                val pigment = MekanismPigments.PIGMENTS.forgeRegistry.entries.first { it.id.toString() == substance.id }.get()
                color = pigment.colorRepresentation
                pigment.textureAtlasSprite
            }
            SLURRY if SLURRY.runtimeRequiredModsIsLoaded() -> {
                val slurry = MekanismSlurries.SLURRIES.forgeRegistry.entries.first { it.id.toString() == substance.id }.get()
                color = slurry.colorRepresentation
                slurry.textureAtlasSprite
            }
            else -> null
        }

        val coverSprite = coverLocation?.let { spriteGetter.apply(it) }
        val particleSprite = particleLocation
            ?.let { spriteGetter.apply(it) }
            ?: substanceSprite
            ?: coverSprite
            ?: error("Couldn't determine particle sprite")

        val transform = modelTransform.rotation
        val transformsMap = PerspectiveMapWrapper.getTransforms(ModelTransformComposition(owner.combinedTransform, modelTransform))

        val builder = ItemMultiLayerBakedModel.builder(
            owner,
            particleSprite,
            ContainedSubstanceOverrideHandler(overrides, bakery, owner, this),
            transformsMap
        )

        if (baseLocation != null) {
            builder.addQuads(
                ItemLayerModel.getLayerRenderType(false),
                ItemLayerModel.getQuadsForSprites(
                    listOf(baseLocation),
                    transform,
                    spriteGetter,
                ),
            )
        }

        if (maskLocation != null && substanceSprite != null) {
            val templateSprite = spriteGetter.apply(maskLocation)
            if (templateSprite != null) {
                builder.addQuads(
                    ItemLayerModel.getLayerRenderType(luminosity > 0),
                    ItemTextureQuadConverter.convertTexture(
                        transform,
                        templateSprite,
                        substanceSprite,
                        NORTH_Z_FLUID,
                        Direction.NORTH,
                        color,
                        1,
                        luminosity
                    )
                )
                builder.addQuads(
                    ItemLayerModel.getLayerRenderType(luminosity > 0),
                    ItemTextureQuadConverter.convertTexture(
                        transform,
                        templateSprite,
                        substanceSprite,
                        SOUTH_Z_FLUID,
                        Direction.SOUTH,
                        color,
                        1,
                        luminosity
                    )
                )
            }
        }

        if (coverSprite != null) {
            builder.addQuads(
                ItemLayerModel.getLayerRenderType(false),
                ItemTextureQuadConverter.genQuad(
                    transform,
                    0f,
                    0f,
                    16f,
                    16f,
                    NORTH_Z_COVER,
                    coverSprite,
                    Direction.NORTH,
                    -0x1,
                    2
                )
            )
            builder.addQuads(
                ItemLayerModel.getLayerRenderType(false),
                ItemTextureQuadConverter.genQuad(
                    transform,
                    0f,
                    0f,
                    16f,
                    16f,
                    SOUTH_Z_COVER,
                    coverSprite,
                    Direction.SOUTH,
                    -0x1,
                    2
                )
            )
        }

        builder.setParticle(particleSprite)

        return builder.build()
    }

    fun IModelConfiguration.resolveTextureOrNull(name: String) = if (isTexturePresent(name)) resolveTexture(name) else null

    override fun getTextures(
        owner: IModelConfiguration,
        modelGetter: Function<ResourceLocation, IUnbakedModel>,
        missingTextureErrors: Set<Pair<String, String>>
    ): Collection<RenderMaterial> {
        val textures = mutableSetOf<RenderMaterial>()

        owner.resolveTextureOrNull("particle")?.also(textures::add)
        owner.resolveTextureOrNull("base")?.also(textures::add)
        owner.resolveTextureOrNull("mask")?.also(textures::add)
        owner.resolveTextureOrNull("overlay")?.also(textures::add)

        return textures
    }

    object Loader : IModelLoader<FluidCellModel> {
        override fun getResourceType() = VanillaResourceType.MODELS

        override fun onResourceManagerReload(resourceManager: IResourceManager) = Unit

        override fun onResourceManagerReload(
            resourceManager: IResourceManager,
            resourcePredicate: Predicate<IResourceType>
        ) = Unit

        override fun read(deserializationContext: JsonDeserializationContext, modelContents: JsonObject) = FluidCellModel(Substance.EMPTY)
    }

    private class ContainedSubstanceOverrideHandler(
        private val nested: ItemOverrideList,
        private val bakery: ModelBakery,
        private val owner: IModelConfiguration,
        private val parent: FluidCellModel
    ) : ItemOverrideList() {
        private val cache = mutableMapOf<String, IBakedModel>()

        override fun resolve(
            originalModel: IBakedModel,
            stack: ItemStack,
            world: ClientWorld?,
            entity: LivingEntity?
        ): IBakedModel? {
            val overriden = nested.resolve(originalModel, stack, world, entity)

            if (overriden !== originalModel) return overriden

            if (stack.item !is FluidCellItemBase) return originalModel

            val substance = stack.substance ?: Substance.EMPTY
            val name = substance.id

            if (name in cache) return cache[name]

            val unbaked = parent.copy(substance = substance)
            val bakedModel = unbaked.bake(
                owner,
                bakery,
                ModelLoader.defaultTextureGetter(),
                ModelRotation.X0_Y0,
                this,
                ResourceLocation(REBAKE_LOCATION)
            )
            cache[name] = bakedModel
            return bakedModel
        }
    }

    data class Substance(
        val id: String,
        val type: Type
    ) {
        enum class Type(vararg val requiresMods: String) {
            FLUID,
            GAS("mekanism"),
            INFUSE("mekanism"),
            PIGMENT("mekanism"),
            SLURRY("mekanism");

            fun runtimeRequiredModsIsLoaded() = requiresMods.all { ModList.get().isLoaded(it) }
        }

        companion object {
            val EMPTY = Substance("minecraft:empty", Type.FLUID)
        }
    }

    companion object {
        val ItemStack.substance: Substance? get() {
            val fluid = fluid
            if (fluid.isNotEmpty) return Substance(fluid.registryName!!.toString(), FLUID)

            if (GAS.runtimeRequiredModsIsLoaded()) {
                val gas = gas
                if (gas.isNotEmpty) return Substance(gas.registryName!!.toString(), GAS)
            }

            if (INFUSE.runtimeRequiredModsIsLoaded()) {
                val infuse = infuse
                if (infuse.isNotEmpty) return Substance(infuse.registryName!!.toString(), INFUSE)
            }

            if (PIGMENT.runtimeRequiredModsIsLoaded()) {
                val pigment = pigment
                if (pigment.isNotEmpty) return Substance(pigment.registryName!!.toString(), PIGMENT)
            }

            if (SLURRY.runtimeRequiredModsIsLoaded()) {
                val slurry = slurry
                if (slurry.isNotEmpty) return Substance(slurry.registryName!!.toString(), SLURRY)
            }

            return null
        }
    }
}