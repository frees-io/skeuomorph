/*
 * Copyright 2018-2019 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package higherkindness.skeuomorph.protobuf

import cats.effect.IO
import higherkindness.skeuomorph.mu.MuF
import higherkindness.skeuomorph.protobuf.ProtobufF._
import higherkindness.skeuomorph.protobuf.ParseProto._
import higherkindness.skeuomorph.mu.CompressionType
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.{ScalaCheck, Specification}
import higherkindness.droste.data.Mu
import higherkindness.droste.data.Mu._

class ProtobufProtocolSpec extends Specification with ScalaCheck {

  val currentDirectory: String                  = new java.io.File(".").getCanonicalPath
  val root                                      = "/src/test/scala/higherkindness/skeuomorph/protobuf"
  val path                                      = currentDirectory + s"$root/service"
  val importRoot                                = Some(currentDirectory + root)
  val source                                    = ProtoSource(s"book.proto", path, importRoot)
  val protobufProtocol: Protocol[Mu[ProtobufF]] = parseProto[IO, Mu[ProtobufF]].parse(source).unsafeRunSync()

  implicit val arbCompressionType: Arbitrary[CompressionType] = Arbitrary {
    Gen.oneOf(CompressionType.Identity, CompressionType.Gzip)
  }

  def is = s2"""
  Protobuf Protocol

  It should be possible to print a protocol from a Proto file. $printProtobufProtocol

  """

  def printProtobufProtocol = prop { (ct: CompressionType, useIdiom: Boolean) =>
    val parseProtocol: Protocol[Mu[ProtobufF]] => higherkindness.skeuomorph.mu.Protocol[Mu[MuF]] = {
      p: Protocol[Mu[ProtobufF]] =>
        higherkindness.skeuomorph.mu.Protocol.fromProtobufProto(ct, useIdiom)(p)
    }

    val printProtocol: higherkindness.skeuomorph.mu.Protocol[Mu[MuF]] => String = {
      p: higherkindness.skeuomorph.mu.Protocol[Mu[MuF]] =>
        higherkindness.skeuomorph.mu.print.proto.print(p)
    }

    val actual   = (parseProtocol andThen printProtocol)(protobufProtocol)
    val expected = expectation(ct, useIdiom)

    (actual.clean must beEqualTo(expected.clean)) :| s"""
      |Actual output:
      |$actual
      |
      |
      |Expected output:
      |$expected"
      """.stripMargin
  }

  def expectation(compressionType: CompressionType, useIdiomaticEndpoints: Boolean): String = {

    val serviceParams: String = "Protobuf" +
      (if (compressionType == CompressionType.Gzip) ", Gzip" else ", Identity") +
      (if (useIdiomaticEndpoints) ", namespace = Some(\"com.acme\"), methodNameStyle = Capitalize" else "")

    s"""package com.acme
      |
      |object book {
      |
      |@message final case class Book(isbn: Long, title: String, author: List[Option[_root_.com.acme.author.Author]], binding_type: Option[_root_.com.acme.book.BindingType], rating: Option[_root_.com.acme.rating.Rating], `private`: Boolean, `type`: Option[_root_.com.acme.book.`type`])
      |@message final case class `type`(foo: Long, thing: Option[_root_.com.acme.`hyphenated-name`.Thing])
      |@message final case class GetBookRequest(isbn: Long)
      |@message final case class GetBookViaAuthor(author: Option[_root_.com.acme.author.Author])
      |@message final case class BookStore(name: String, books: Map[Long, String], genres: List[Option[_root_.com.acme.book.Genre]], payment_method: Cop[Long :: Int :: String :: _root_.com.acme.book.Book :: TNil])
      |
      |sealed trait Genre
      |object Genre {
      |  case object UNKNOWN extends _root_.com.acme.book.Genre
      |  case object SCIENCE_FICTION extends _root_.com.acme.book.Genre
      |  case object POETRY extends _root_.com.acme.book.Genre
      |}
      |
      |
      |sealed trait BindingType
      |object BindingType {
      |  case object HARDCOVER extends _root_.com.acme.book.BindingType
      |  case object PAPERBACK extends _root_.com.acme.book.BindingType
      |}
      |
      |@service($serviceParams) trait BookService[F[_]] {
      |  def GetBook(req: _root_.com.acme.book.GetBookRequest): F[_root_.com.acme.book.Book]
      |  def GetBooksViaAuthor(req: _root_.com.acme.book.GetBookViaAuthor): Stream[F, _root_.com.acme.book.Book]
      |  def GetGreatestBook(req: Stream[F, _root_.com.acme.book.GetBookRequest]): F[_root_.com.acme.book.Book]
      |  def GetBooks(req: Stream[F, _root_.com.acme.book.GetBookRequest]): Stream[F, _root_.com.acme.book.Book]
      |}
      |
      |}""".stripMargin
  }

  implicit class StringOps(self: String) {
    def clean: String = self.replaceAll("\\s", "")
  }

}
