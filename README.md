# Robolectric UI Test Samples by Using Espresso Idling Resource

[DroidKaigi 2020](https://droidkaigi.jp/2020/) で発表する予定だった「[Robolectricの限界を理解してUIテストを高速に実行しよう](https://droidkaigi.jp/2020/timetable/156794)」のサンプルコードです。

本リポジトリは、
GoogleがAndroid Jetpackのサンプルアプリとして開発している[Android Sunflower](https://github.com/googlesamples/android-sunflower)に、Robolectricで動作するEspressoで書かれたテストコードを追加したものです。

あわせて、本リポジトリには、[EspressoのIdlingResource](https://developer.android.com/training/testing/espresso/idling-resource)をサポートするように改造したRobolectricが含まれています。
IdlingResource対応のRobolectricを試してみたい方は、
後述の「[IdlingResource対応のRobolectricを試してみるには](#robolectric-idlingresource)」を参照してください。

Android Sunflower付属のオリジナルのREADMEは、[README.orig.md](README.orig.md)を参照してください。

## 動作確認環境

- Android Studio 3.5.3

## オリジナルのAndroid Sunflowerとの違い

- ライブラリの依存関係を最新化しています。
  それに伴い、最新のライブラリでビルドできるように`PlantDetailFragment.kt`と`MaskedCardView.kt`を修正しています。
- オリジナルのREADME.mdのファイル名をREADME.orig.mdにリネームしています。
- 既存のテストを削除しています。
- Espressoで書いたUIテストを`src/androidTest`と`src/test`の両方に追加しています。
- Instrumented TestからもLocal Testからも参照できる`src/sharedTest`ディレクトリを追加しています。
- 植物が庭に追加されるときに、わざとバックグラウンドスレッドで3秒スリープするように変更しています。
  また、そのときに実行されるスレッドをテストコードから変更できるようにしています。  
  (`PlantDetailViewModel.kt`)
- 一度保持した`AppDatabase`インスタンスを、テストコードから破棄できるようにしています。  
  (`AppDatabase.kt`)
- 一度保持したDAOインスタンスを、テストコードから差し替えられるようにしています。
  (`GardenPlantingRepository.kt`と`PlantRepository.kt`)

## テストコードの概要

Espresso Test Recorderで記録したテスト(一部改変あり)を、
`Instrumented Test`・`Robolectricを使ったLocal Test`の両方で動作するようにしています。
テストの内容は次の通りで、`Mango`を選ぶものと`Eggplant`を選ぶものの2つのケースが存在しています。

1. `Add Plant`を押して`Plant List`画面に遷移する
2. リストされている植物からを1つ選んで、FABを押して追加する
3. `My Garden`画面に戻って、追加した植物が表示されていることを確認する。

テストのエントリーポイントは次の通りです。

- Instrumented Test: [`src/androidTest/java/com/google/samples/apps/sunflower/GardenActivityTest2.kt`](https://github.com/sumio/robolectric-espresso-samples/blob/master/app/src/androidTest/java/com/google/samples/apps/sunflower/GardenActivityTest2.kt)
- Local Test: [`src/test/java/com/google/samples/apps/sunflower/RobolectricGardenActivityTest2.kt`](https://github.com/sumio/robolectric-espresso-samples/blob/master/app/src/test/java/com/google/samples/apps/sunflower/RobolectricGardenActivityTest2.kt)

実際にEspressoのAPIを使って画面を操作している部分はPage Object化し、両方のテストから参照できる  
[`src/sharedTest/java/com/google/samples/apps/sunflower/`](https://github.com/sumio/robolectric-espresso-samples/tree/master/app/src/sharedTest/java/com/google/samples/apps/sunflower)`{page,util}/`  
配下に配置しています。

## <a id="robolectric-idlingresource"></a> IdlingResource対応のRobolectricを試してみるには

本リポジトリには、[EspressoのIdlingResource](https://developer.android.com/training/testing/espresso/idling-resource)をサポートするように改造したRobolectricが含まれています。
ご自身のプロジェクトに、IdlingResource対応のRobolectricを適用するには、次の手順にしたがってください。

### ファイルのコピー

次のディレクトリを、本リポジトリから、適用したいプロジェクトにコピーしてください。
コピー先も同じディレクトリ構成にしてください。

- `app/local-repo`
- `app/src/test/resources`
- `app/src/test/java/androidx`

### build.gradleファイルの編集

1. `app/local-repo`を、mavenのリポジトリ参照先として追加してください  
   
   ```groovy
   // app/build.gradleに追記する場合
   repositories {
       maven { url = file('local-repo') }
   }
   ```
2. 依存関係に宣言されているRobolectricのバージョン表記を`4.3.1-modified`にしてください  
   
   ```groovy
   dependencies {
       ...
       testImplementation "org.robolectric:robolectric:4.3.1-modified"
       ...
   }
   ```

以上で、IdlingResourceに対応したRobolectricが使えるようになります。

### <a id="notice-idlingresource"></a> Idling Resource対応についての注意事項

- この対応は限られたケースで動作確認したに過ぎません。その点ご理解の上お試しください。
- [`IdlingRegistry.registerLooperAsIdlingResource()`](https://developer.android.com/reference/androidx/test/espresso/IdlingRegistry.html?hl=en#registerLooperAsIdlingResource%28android.os.Looper%29)を使ったケースは未確認です。恐らく対応できていないと思います。

## Robolectric対応のポイント

### RobolectricのLooper Mode

Robolectric 4.3から導入された`PAUSED` Looper Modeにしています。
Looper Modeについての詳細は[Improving Robolectric's Looper simulation](http://robolectric.org/blog/2019/06/04/paused-looper/)を参照してください。

```kotlin
@RunWith(AndroidJUnit4::class)
@LooperMode(LooperMode.Mode.PAUSED)
class RobolectricGardenActivityTest2 {
    ...
}
```

### WorkManager対応

Robolectricで動かす場合、デフォルトのinitializerでは動作しません。
そのため、[デフォルトのinitializerを削除](https://developer.android.com/topic/libraries/architecture/workmanager/advanced/custom-configuration#remove-default)し、[`WorkManagerTestInitHelper`](https://developer.android.com/reference/kotlin/androidx/work/testing/WorkManagerTestInitHelper.html)を使って初期化しています。

デフォルトのinitializerを削除している箇所は[`src/test/AndroidManifest.xml`](https://github.com/sumio/robolectric-espresso-samples/blob/master/app/src/test/AndroidManifest.xml)です。

初期化コードは、テスト専用のアプリケーションクラス[`TestApplication`](https://github.com/sumio/robolectric-espresso-samples/blob/master/app/src/test/java/com/google/samples/apps/sunflower/TestApplication.kt)を定義し、その`onCreate()`の中で実行しています。
Robolectricでは、テスト専用のアプリケーションクラスを次のように指定することができます。

```kotlin
@Config(application = TestApplication::class)
class RobolectricGardenActivityTest2 {
    ...
}    
```

### Idling Resource対応

Robolectricの現バージョンでは、EspressoのIdling Resourceに[対応していません](https://github.com/robolectric/robolectric/issues/4807)。そのため、このサンプルでは独自実装によってRobolectricでIdling Resourceを待ち合わせるようにしてあります。

EspressoでIdling Resourceがアイドル状態になるのを待ち合わせている箇所は`UiController`インターフェイスを実装した[`UiControllerImpl`](https://github.com/android/android-test/blob/androidx-test-1.2.0/espresso/core/java/androidx/test/espresso/base/UiControllerImpl.java)です。

一方で、Robolectricが提供している`UiController`インターフェイスの実装は[`LocalUiController`](https://github.com/robolectric/robolectric/blob/robolectric-4.3.1/robolectric/src/main/java/org/robolectric/android/internal/LocalUiController.java)で、こちらにはIdling Resourceを待ち合わせているコードがありません。

そこで`LocalUiController`を拡張した[`IdlingLocalUiController`](https://github.com/sumio/robolectric-espresso-samples/blob/master/app/src/test/java/androidx/test/espresso/base/IdlingLocalUiController.java)を実装し、RobolectricでもIdling Resourceを待ち合わせるようにしました。

具体的には、Espressoの`UiControllerImpl`のうち、Idling Resourceを待ち合わせているロジックだけを`IdlingLocalUiController`に移植しています。その差分は[このコミット](https://github.com/sumio/robolectric-espresso-samples/pull/2/commits/40f8e1cf044d61ac0078f0f89c79e96af7339c76)を参照してください。

なお、Robolectricが提供する`UiController`インターフェイスの実装は、JARファイルの
[`META-INF/services/androidx.test.platform.ui.UiController`](https://github.com/robolectric/robolectric/blob/robolectric-4.3.1/robolectric/src/main/resources/META-INF/services/androidx.test.platform.ui.UiController)にハードコードされているため、RobolectricのJARファイルにも手を加えざるを得ませんでした。

手を加えたRobolectricは[`app/local-repo`ディレクトリ配下](https://github.com/sumio/robolectric-espresso-samples/tree/master/app/local-repo/org/robolectric/robolectric/4.3.1-modified)に格納しています。
オリジナルとの差分は`META-INF/services/androidx.test.platform.ui.UiController`を削除した点のみです。  
(後日、Robolectric本家にPRできればと考えています)

この対応のための修正は、 [#2](https://github.com/sumio/robolectric-espresso-samples/pull/2) にまとまっていますので、興味のある方は参考にしてみてください。

#### 注意事項

このIdlingResource対応は限られたケースで動作確認したに過ぎません。
前述の「[Idling Resource対応についての注意事項](#notice-idlingresource)」をご理解の上お試しください。

### Room対応

Robolectricでは、テスト独立性を高めるために、テスト終了時にデータベースファイルを削除する仕様となっています。

そのため、ビルドした`RoomDatabase`のインスタンスを、テストをまたがって保持する設計になっている場合、
2回目のテストからは存在しないデータベースファイルを参照することになり、データベースアクセスが正しく動作しません。

次のようにすることで、このRobolectricの仕様に対応することができます。
この対応にはアプリ側にも手を入れざるを得ないケースが多いと思います。

- Robolectric用に用意した`Application`クラスで、毎回`RoomDatabase`をビルドするようにします。
- アプリケーション内で以下の参照を保持し続ける実装になっている場合は、`RoomDatabase`をビルドしたタイミングで、新しい参照に更新するようにします。
  - ビルドした`RoomDatabase`のインスタンス
  - DAOのインスタンス

具体的な修正内容は [#3](https://github.com/sumio/robolectric-espresso-samples/pull/3) のうち、以下の箇所を参照してください。

- `AppDatabase.kt`
- `GardenPlantingRepository.kt`
- `PlantRepository.kt`
- `RobolectricGardenActivityTest2.kt`の`tearDown()`で`appDatabase`の`close()`・`clear()`を呼び出している箇所

### Robolectricで動作しない機能について

次の機能は、Robolectricでは動作しないことが確認できています。
ここに挙げた機能を使った部分については、Robolectricによるテストを避けた方が無難です。

- [Navigation Drawer](https://material.io/components/navigation-drawer/)内のメニュー操作
- [Paging Library](https://developer.android.com/topic/libraries/architecture/paging)を使っているRecyclerViewの操作
- Espressoの[`DrawerActions`](https://developer.android.com/reference/androidx/test/espresso/contrib/DrawerActions?hl=en) API

## ライセンス

Original Copyright 2018 Google, Inc. See [README.orig.md](README.orig.md) for details.

Modifications Copyright 2020 TOYAMA Sumio &lt;jun.nama@gmail.com&gt;  

Licensed under the
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

### Third Party Content

- Select text used for describing the plants (in `plants.json`) are used from Wikipedia via CC BY-SA 3.0 US (license in `ASSETS_LICENSE`).
- "[seed](https://thenounproject.com/search/?q=seed&i=1585971)" by [Aisyah](https://thenounproject.com/aisyahalmasyira/) is licensed under [CC BY 3.0](https://creativecommons.org/licenses/by/3.0/us/legalcode)
- [`robolectric-4.3.1-modified.jar`](https://github.com/sumio/robolectric-espresso-samples/tree/master/app/local-repo/org/robolectric/robolectric/4.3.1-modified) is a modified version of [Robolectric 4.3.1](https://github.com/robolectric/robolectric/releases/tag/robolectric-4.3.1) licensed under the Apache License, Version 2.0.
- [`IdlingLocalUiController.java`](https://github.com/sumio/robolectric-espresso-samples/blob/master/app/src/test/java/androidx/test/espresso/base/IdlingLocalUiController.java) is a modified version of [`UiControllerImpl.java`](https://github.com/android/android-test/blob/androidx-test-1.2.0/espresso/core/java/androidx/test/espresso/base/UiControllerImpl.java) licensed under the Apache License, Version 2.0.
- [`PausedLooperInterrogator.java`](https://github.com/sumio/robolectric-espresso-samples/blob/master/app/src/test/java/androidx/test/espresso/base/PausedLooperInterrogator.java) is a modified version of [`Interrogator.java`](https://github.com/android/android-test/blob/androidx-test-1.2.0/espresso/core/java/androidx/test/espresso/base/Interrogator.java) licensed under the Apache License, Version 2.0.
- [`TaskExecutorWithIdlingResourceRule.kt`](https://github.com/sumio/robolectric-espresso-samples/blob/master/app/src/sharedTest/java/com/android/example/github/util/TaskExecutorWithIdlingResourceRule.kt) is copied from [GithubBrowserSample](https://github.com/android/architecture-components-samples/blob/1d7a759f742e8bdaf1eb4531e38ea9270301c577/GithubBrowserSample/app/src/androidTest/java/com/android/example/github/util/TaskExecutorWithIdlingResourceRule.kt) licensed under the Apache License, Version 2.0.
- [`DataBindingIdlingResource.kt`](https://github.com/sumio/robolectric-espresso-samples/blob/master/app/src/sharedTest/java/com/example/android/architecture/blueprints/todoapp/util/DataBindingIdlingResource.kt) is copied from [Android Architecture Blueprints v2](https://github.com/android/architecture-samples/blob/b9518b1c20affeea9fb8f0b75d153659519c5f58/app/src/sharedTest/java/com/example/android/architecture/blueprints/todoapp/util/DataBindingIdlingResource.kt) licensed under the Apache License, Version 2.0.
