import java.util
import processing.core._
import processing.serial._
import scala.util.Random

class App extends PApplet {

  var myPort: Serial = _
  var str: String = _
  var firstContact = false
  val recipeSize = 10
  var recipe: util.Stack[Char] = genRecipe()
  var prevInput = ""
  var winState = false
  var loseState = false
  var wrote = false

  val maxPower: Float = 1021

  val base: Array[Array[Char]] = Array (
    Array(' ','|','A','A','A','A','A','A','A',' ','|',' ',' ','|','B','B','B','B','B','B','B',' ','|',' ',' ','|','C','C','C','C','C','C','C',' ','|',' '),
    Array(' ','=','=','=','=','=','=','=','=','=','=',' ',' ','=','=','=','=','=','=','=','=','=','=',' ',' ','=','=','=','=','=','=','=','=','=','=',' ')
  )
  val doorMan = new DoorMan(base)

  override def setup(): Unit = {
    size(400, 400)
    val portName: String = Serial.list()(0)
    myPort = new Serial(this, portName, 9600)
    myPort.bufferUntil('\n')
  }

  override def draw(): Unit = {
    background(255)
    val f = createFont("Monospaced",16,true)
    textFont(f, 16)
    fill(0)
    if (winState) {
      text("You win!\nReset potentiometer and press both\nbuttons to play again", 10, 100)
    } else if (loseState) {
      text("You Lose!\nReset potentiometer and press both\nbuttons to play again", 10, 100)
    } else {
      text(toString(), 10, 10)
    }
  }

  def serialEvent(myPort: Serial): Unit = {
    str = myPort.readStringUntil('\n')
    if (str != null) {
      str = str.trim
      if (!firstContact) {
        makeContact()
      } else {
        parse(str)
      }
    }
  }

  def genRecipe(): util.Stack[Char] = {
    val rand = new Random
    val stack = new util.Stack[Char]
    for (_ <- 0 until recipeSize) {
      rand.nextInt(3) match {
        case 0 => stack.push('A')
        case 1 => stack.push('B')
        case 2 => stack.push('C')
      }
    }
    stack
  }

  override def toString: String = {
    val stringBuilder = new StringBuilder

    for (row <- doorMan.state) {
      for (char <- row) {
        stringBuilder.append(char)
      }
      stringBuilder.append("\n")
    }

    for (j <- 0 until recipeSize) {
      var i = recipeSize - j - 1
      if (i >= recipe.size()) {
        stringBuilder.append("| |\n")
      } else {
        stringBuilder.append("|").append(recipe.elementAt(i)).append("|")
        if (i + 1 == recipe.size()) {
          stringBuilder.append("<-\n")
        }
        else stringBuilder.append("\n")
      }
    }
    stringBuilder.toString()
  }

  def makeContact(): Unit = {
    if (str.equals("A")) {
      myPort.clear()
      firstContact = true
      myPort.write("A")
      println("contact")
    }
  }

  def parse(str: String): Unit = {
    if (winState || loseState) {
      if (str == "LR") {
        reset()
        prevInput = "LR"
      }
    } else {
      str match {
        case "LR" =>
          println("reseting")
          if (prevInput != "LR") {
            reset()
            prevInput = "LR"
          }
        case "N" =>
          prevInput = ""
        case "lose" =>
          println("out of time")
          loseState = true;
        case x =>
          val dropped = doorMan.set(x.split("\\."), maxPower / 10)
          for ((n, i) <- dropped.zipWithIndex) {
            if (n != 0) {
              for (_ <- 1 to n) {
                if (!recipe.empty() && (doorMan.getDoor(i).charType != recipe.peek())) {
                  loseState = true
                  println(s"Expected ${recipe.peek()}, got ${doorMan.getDoor(i).charType}")
                  if (!wrote) {
                    myPort.write('l')
                    wrote = true
                  }
                } else if (recipe.empty()) {
                  winState = true
                  if (!wrote) {
                    myPort.write('w')
                    wrote = true
                  }
                } else {
                  recipe.pop()
                  if (recipe.empty()) {
                    winState = true
                    if (!wrote) {
                      myPort.write('w')
                      wrote = true
                    }
                  }
                }
              }
            }
          }
      }
    }
  }

  def reset(): Unit = {
    recipe = genRecipe()
    doorMan.reset()
    winState = false
    loseState = false
    wrote = false
  }

}

object App extends PApplet {

  private var app: App = _

  def main(args: Array[String]): Unit = {
    app = new App
    val frame = new javax.swing.JFrame("Test")
    frame.getContentPane.add(app)
    app.init()
    frame.pack()
    frame.setVisible(true)
  }

}