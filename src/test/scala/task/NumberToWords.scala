package task

object NumberToWords {

  def main(args: Array[String]): Unit = {
    println(apply(123456))
  }

  private val singleDigit = Array("zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine")

  private val tensDigits = Array("", "ten", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")

  private val elevenToNineteenDigits = Array("", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen")

  def apply(number: Int): String =
    if (number >= 0 && number <= 9) { //0,1,...9
      convertSingleDigit(number)
    }
    else if (number >= 10 && number <= 90 && number % 10 == 0) { //10,20,...90
      convertTensDigits(number)
    }
    else if (number >= 11 && number <= 19) { //11,12,...19
      convert11To19Digits(number)
    }
    else if (number >= 21 && number <= 99) { //21,22,...99
      convertRestTwoDigits(number)
    }
    else if (number >= 100 && number <= 999) { //100,101,...999
      convertHundreds(number)
    }
    else if (number >= 1000 && number <= 999999) { //1000,...999999
      convertThousands(number)
    }
    else if (number >= 1000000 && number <= 999999999) { //1000000,...999999999
      convertMilions(number)
    }
    else if (number >= 1000000000 && number <= 2147483647) { //1000000000,...2147483647
      convertBilions(number)
    }
    else if (number <= -1 && number >= -2147483648) { //minus -1,...-2147483648
      "minus " + apply(Math.abs(number))
    }
    else throw new IllegalArgumentException("NOT AN INTEGER")

  private def convertSingleDigit(number: Int) = singleDigit(number)

  private def convertTensDigits(number: Int) = tensDigits(number / 10)

  private def convert11To19Digits(number: Int) = elevenToNineteenDigits(number - 10)

  private def convertRestTwoDigits(number: Int) = tensDigits(number / 10) + " " + singleDigit(number % 10)

  private def convertHundreds(number: Int) = {
    var hundredResult = singleDigit(number / 100) + " hundred"
    val remaining     = number % 100
    if (remaining != 0) hundredResult += " " + apply(remaining)
    hundredResult
  }

  private def convertThousands(number: Int) = {
    val thousand       = number / 1000
    val thousandResult = apply(thousand) + " thousand"
    val remaining      = number % 1000
    qweqwe(thousandResult, remaining)
  }

  private def convertMilions(number: Int) = {
    val million       = number / 1000000
    val millionResult = apply(million) + " million"
    val remaining     = number % 1000000
    qweqwe(millionResult, remaining)
  }

  private def convertBilions(number: Int) = {
    val billion       = number / 1000000000
    val billionResult = apply(billion) + " billion"
    val remaining     = number % 1000000000
    qweqwe(billionResult, remaining)
  }

  private def qweqwe(billionResult: String, remaining: Int) = {
    if (remaining != 0) billionResult + " and " + apply(remaining)
    else billionResult
  }
}
