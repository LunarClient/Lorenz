val projectName: String by settings
rootProject.name = projectName

include(
    "lorenz",
    "lorenz-io-enigma",
    "lorenz-io-jam",
    "lorenz-io-kin",
    "lorenz-io-proguard"
)
