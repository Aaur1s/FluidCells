// priority: 0

onEvent("item.registry", event => {
    event.create("example_cell")
        // if not specified, will be just item
        .type("fluidcells:custom")
        // size of cell in mb (if not 1000 and not fractional it won't be able to collect/place fluid blocks)
        .sizeMb(2147483647)
        // should cell support not full/empty values of fluid; is Universal cell in old IC2 langauge
        .isFractional(true)
        // name of mask for fluid, by default equal to item name
        .fluidMaskName("kubejs:example_fluid_mask")
        .displayName("Example Cell");
})