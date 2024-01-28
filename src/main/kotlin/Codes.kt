import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import java.io.InputStream
import java.util.*


fun getCourses(username: String, password: String, targetUrl: String): List<Course> {
    // Create a ChromeDriver instance
    val option = ChromeOptions()
    option.addArguments("--headless")

    val driver = ChromeDriver(option)
    var courses = listOf<Course>()

    try {
        // Navigate to the target URL
        driver.get(targetUrl)

        // Locate elements and perform actions
        val usernameInput: WebElement = driver.findElement(By.id("ctl06_txtKullaniciAdi"))
        val passwordInput: WebElement = driver.findElement(By.id("ctl06_txtSifre"))
        val loginButton = driver.findElement(By.className("btn-primary"))

        usernameInput.sendKeys(username)
        passwordInput.sendKeys(password)
        loginButton.click()
        courses = getGrades(driver)

        courses.forEach {
            println(it.name)
            println("arasınav = ${it.araSinav}")
            println("final = ${it.final}")
            println("butunleme = ${it.butunleme}")
            println("grade = ${it.grade}")
        }

    } finally {
        // Close the ChromeDriver instance
        driver.quit()
    }
    return courses
}

fun getGrades(driver: ChromeDriver): List<Course> {
    val gradesUrl = "https://derskayit.cu.edu.tr/Ogrenci/SecilenDersler"
    driver.get(gradesUrl)

    // Sayfanın HTML içeriğini al
    val pageSource = driver.pageSource

    // Jsoup kullanarak HTML String'ini Document nesnesine çevir
    val document = Jsoup.parse(pageSource)

    // Document nesnesini kullanarak elementleri seç
    val courses = document.select("h5")
    val notes = document.select(".text-center td:nth-of-type(3)")
    val grades = document.select("tr:nth-of-type(n+3) td:nth-of-type(1)")
    grades.removeIf { it.text().toIntOrNull() == 60 }


    // Ders nesnelerini oluşturmak için kullanılacak liste
    val courseList = mutableListOf<Course>()

    // Dersler arasında dolaşarak Course nesnelerini oluştur
    var notesIndex = 0

    for (i in courses.indices) {
        val courseName = courses[i].text()
        val grade = grades[i].text()
        val note = mutableListOf<Int>()

        if (grade != "FF" && grade != "N/A" && grade != "FG") {
            // Notları al, en fazla 2 notu al
            note.add(notes.getOrNull(notesIndex++)?.text()?.toIntOrNull() ?: -1)
            note.add(notes.getOrNull(notesIndex++)?.text()?.toIntOrNull() ?: -1)
            val course = Course(name = courseName, araSinav = note[0], final = note[1], grade = grade)
            courseList.add(course)
        } else {
            // FF, N/A veya FG ise bir sonraki 3 notu al
            note.add(notes.getOrNull(notesIndex++)?.text()?.toIntOrNull() ?: -1)
            note.add(notes.getOrNull(notesIndex++)?.text()?.toIntOrNull() ?: -1)
            note.add(notes.getOrNull(notesIndex++)?.text()?.toIntOrNull() ?: -1)
            val course =
                Course(name = courseName, araSinav = note[0], final = note[1], butunleme = note[2], grade = grade)
            courseList.add(course)
        }
    }

    return courseList
}

@OptIn(InternalAPI::class)
suspend fun sendMessage(botToken: String, chatId: String, courses: MutableList<Course>) {
    for (course in courses) {

        val messageText = buildMessageText(course)
        val apiUrl = Url("https://api.telegram.org/bot$botToken/sendMessage?chat_id=${chatId}&text=${messageText}")


        val client = HttpClient(OkHttp) {
            // HttpClient yapılandırması
            defaultRequest {
                contentType(ContentType.Application.Json)
            }
        }
        coroutineScope {
            try {
                // Telegram API'ye POST isteği yap
                val response = client.post(apiUrl) {

                }

                // Response'ı kontrol et
                println("Telegram API response: $response")
            } catch (e: Exception) {
                println("Error sending message to Telegram: ${e.message}")
            } finally {
                // HttpClient'ı kapat
                client.close()
            }
        }
    }


}

fun buildMessageText(course: Course): String {
    val stringBuilder = StringBuilder()

    // Mesaj metni oluştur
    if (course.final != -1) {
        stringBuilder.append("Değişiklikler tespit edildi!\n")
        stringBuilder.append("${course.name}: \n Arasınav=${course.araSinav}, \n Final=${course.final},\n Bütünleme=${course.butunleme}, \nNot=${course.grade}\n")

    }
    return stringBuilder.toString()
}

fun readCredentials(): Properties {
    val properties = Properties()

    try {
        // Dosyanın adı
        val fileName = "credentials.txt"

        // ClassLoader kullanarak dosyayı yükleyin
        val inputStream: InputStream? = object {}.javaClass.classLoader.getResourceAsStream(fileName)

        // Properties nesnesine yükleyin
        inputStream?.use {
            properties.load(it)
        } ?: throw RuntimeException("Credentials file not found: $fileName")

    } catch (e: Exception) {
        e.printStackTrace()
    }

    return properties
}

suspend fun execute(isRunning: Boolean) {
    val credentials = readCredentials()
    val username = credentials["USERNAME"].toString()
    val password = credentials["PASSWORD"].toString()
    val targetUrl = credentials["TARGET_URL"].toString()
    val telegramBotToken = credentials["TELEGRAM_BOT_TOKEN"].toString()
    val telegramChatId = credentials["TELEGRAM_CHAT_ID"].toString()

    if (username != null && password != null && targetUrl != null && telegramBotToken != null && telegramChatId != null) {
        val chromeDriverPath = "path/to/chromedriver"
        System.setProperty("webdriver.chrome.driver", chromeDriverPath)
        val printedCourses = mutableListOf<Course>()

        while (isRunning) {
            val courses = getCourses(username, password, targetUrl)
            val changedCourses = courses.filter {
                it.final != -1 || it.butunleme != -1

            }

            val notPrintedCourses = changedCourses.filter { course ->
                !printedCourses.any { it.name == course.name }
            }as MutableList


        if (notPrintedCourses.isNotEmpty()) {
            sendMessage(botToken = telegramBotToken, chatId = telegramChatId, notPrintedCourses)
            printedCourses.addAll(notPrintedCourses)
        }
        // 1800000 milisaniye (30 dakika) beklet
        delay(1000)
    }

} else {
    println("Credentials are missing or invalid.")
}
}






