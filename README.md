# 🧩 Morphe Patches for Polish Apps

Morphe patches for Polish Android apps.

## ❓ About

A collection of [Morphe](https://morphe.software) patches for apps popular in Poland.
Patches are grouped by app under `patches/src/main/kotlin/app/polishapps/patches/<app>/`.

### Supported apps

#### Yanosik (`pl.neptis.yanosik.mobi.android`)

| Patch | Description | Default |
| --- | --- | --- |
| **Remove Yanosik ads** | Removes banner ads, splash/start adverts and advert-based navigation POIs. | ✅ |
| **Unlock Yanosik PRO** | Reports an active premium entitlement and unlocks the PRO-only settings (view after launch / "Widok po uruchomieniu", floating icon / "Pływająca ikona"). | ✅ |
| **Remove Yanosik radio** | Removes the built-in Radio Yanosik from the main screen and the map button. | ✅ |

### How to use these patches

Click here to add these patches to Morphe: https://morphe.software/add-source?github=RikoDEV/morphed-pl

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
> **[v1.1.0](https://github.com/RikoDEV/morphed-pl/releases/tag/v1.1.0)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;4 patches total
<details open>
<summary>📦 Yanosik&nbsp;&nbsp;•&nbsp;&nbsp;4 patches</summary>
<br>

**🎯 Supported versions:**

| 26.9.0 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Remove Yanosik ads](#remove-yanosik-ads) | Removes banner ads, splash/start adverts and advert-based navigation POIs. |  |
| [Remove Yanosik map bottom shelf](#remove-yanosik-map-bottom-shelf) | Removes the map bottom sheet/panel (search bar, shortcuts, home and road help buttons). |  |
| [Remove Yanosik radio](#remove-yanosik-radio) | Removes the built-in Radio Yanosik from the main screen and the map button. |  |
| [Unlock Yanosik PRO](#unlock-yanosik-pro) | Reports an active premium entitlement and unlocks the PRO-only settings (view after launch, floating icon). |  |

</details>

<!-- PATCHES_END -->

## ⚠️ Version support

### Yanosik

The patches currently target **26.9.0 (arm64-v8a, version code 6001512)**
only, because the obfuscated class names they reference (`r5f`, `l9f`, `tmd`, `m8b`, …) change between
releases. When updating to a new version, re-check the fingerprints in:

- `patches/src/main/kotlin/app/polishapps/patches/yanosik/ads/RemoveAdsPatch.kt`
- `patches/src/main/kotlin/app/polishapps/patches/yanosik/premium/UnlockProPatch.kt`
- `patches/src/main/kotlin/app/polishapps/patches/yanosik/radio/RemoveRadioPatch.kt`

> The radio patch is the most version-robust: it flips the availability flag
> `DashboardRadioConfiguration.a()`, whose class/method names are not obfuscated.

## 🚀 Releases & CI

Everything is automated from the **`main`** branch:

- Use [Conventional Commits](https://www.conventionalcommits.org/): `feat:` → minor, `fix:` / `perf:` → patch, `chore:` / `docs:` → no release.
- A `feat`/`fix` push to `main` runs [`.github/workflows/release.yml`](.github/workflows/release.yml),
  which builds the `.mpp`, updates `patches-list.json`, `patches-bundle.json`, `CHANGELOG.md` and the
  patches list in this README, then publishes a GitHub release with the `.mpp` attached.
- Commits that don't trigger a release just verify the project compiles.
- Pull requests and non-`main` branches run [`.github/workflows/build.yml`](.github/workflows/build.yml)
  to verify the patches build.

The Gradle build resolves the `app.morphe.patches` plugin from the Morphe registry, so CI passes
`GITHUB_TOKEN` (with `packages: read`) automatically.

## 🛠️ Building locally

- Run `./gradlew buildAndroid`
- The built patches `.mpp` file is found in `patches/build/libs/patches-*.mpp`
- Patch an app with [Morphe Desktop](https://github.com/MorpheApp/morphe-desktop) or the Morphe CLI:

```
java -jar cli.jar patch --patches patches-*.mpp Yanosik_26.9.0.apkm
```

> Building requires a GitHub personal access token with `read:packages` for the Morphe registry
> (`gpr.user` / `gpr.key` Gradle properties or `GITHUB_ACTOR` / `GITHUB_TOKEN` environment variables).
> See the [Morphe documentation](https://github.com/MorpheApp/morphe-documentation).

## ➕ Adding another app

1. Add a `Compatibility` constant for the app in
   `patches/src/main/kotlin/app/polishapps/patches/shared/Constants.kt`.
2. Create a package `patches/src/main/kotlin/app/polishapps/patches/<app>/`.
3. Add the patch(es) there using the shared helpers in `.../shared/Utils.kt`.

## 📜 License

Morphe Patches for Polish Apps are licensed under the [GNU General Public License v3.0](LICENSE).

They are built on the [Morphe patches template](https://github.com/MorpheApp/morphe-patches-template),
[crimera/piko](https://github.com/crimera/piko) and the [ReVanced](https://github.com/ReVanced) prior work.
