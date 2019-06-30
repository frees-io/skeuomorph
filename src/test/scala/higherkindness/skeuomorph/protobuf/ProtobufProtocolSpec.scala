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
import qq.droste.data.Mu
import org.specs2.Specification
import higherkindness.skeuomorph.protobuf.ParseProto._
import qq.droste.data.Mu._

class ProtobufProtocolSpec extends Specification {

  def is = s2"""
  Protobuf Protocol

  It should be possible to print a protocol from a Proto file. $printProtobufProtocol

  """

  def printProtobufProtocol = {

    val currentDirectory: String                  = new java.io.File(".").getCanonicalPath
    val path                                      = currentDirectory + "/src/test/scala/higherkindness/skeuomorph/protobuf"
    val source                                    = ProtoSource(s"book.proto", path)
    val protobufProtocol: Protocol[Mu[ProtobufF]] = parseProto[IO, Mu[ProtobufF]].parse(source).unsafeRunSync()

    def parseProtocol(
        useIdiomaticEndpoints: Boolean): Protocol[Mu[ProtobufF]] => higherkindness.skeuomorph.mu.Protocol[Mu[MuF]] = {
      p: Protocol[Mu[ProtobufF]] =>
        higherkindness.skeuomorph.mu.Protocol.fromProtobufProto(useIdiomaticEndpoints)(p)
    }

    val printProtocol: higherkindness.skeuomorph.mu.Protocol[Mu[MuF]] => String = {
      p: higherkindness.skeuomorph.mu.Protocol[Mu[MuF]] =>
        higherkindness.skeuomorph.mu.print.proto.print(p)
    }

    def check(useIdiomaticEndpoints: Boolean) =
      (parseProtocol(useIdiomaticEndpoints) andThen printProtocol)(protobufProtocol).clean must beEqualTo(
        expectation(useIdiomaticEndpoints).clean)

    List(false, true).forall(check)
  }

  def expectation(useIdiomaticEndpoints: Boolean): String = {

    val serviceParams: String = "Protobuf, Identity" + (
      if (useIdiomaticEndpoints) ", namespace = Some(\"com.acme\"), methodNameStyle = Capitalize"
      else ""
    )

    s"""package com.acme
      |
      |import com.acme.author.Author
      |
      |object book {
      |
      |@message final case class Book(isbn: Long, title: String, author: List[Option[Author]], binding_type: Option[BindingType])
      |@message final case class GetBookRequest(isbn: Long)
      |@message final case class GetBookViaAuthor(author: Option[Author])
      |@message final case class BookStore(name: String, books: Map[Long, String], genres: List[Option[Genre]], payment_method: Cop[Long :: Int :: String :: Option[Book]:: TNil])
      |
      |sealed trait Genre
      |object Genre {
      |  case object UNKNOWN extends Genre
      |  case object SCIENCE_FICTION extends Genre
      |  case object POETRY extends Genre
      |}
      |
      |
      |sealed trait BindingType
      |object BindingType {
      |  case object HARDCOVER extends BindingType
      |  case object PAPERBACK extends BindingType
      |}
      |
      |@service($serviceParams) trait BookService[F[_]] {
      |  def GetBook(req: GetBookRequest): F[Book]
      |  def GetBooksViaAuthor(req: GetBookViaAuthor): Stream[F, Book]
      |  def GetGreatestBook(req: Stream[F, GetBookRequest]): F[Book]
      |  def GetBooks(req: Stream[F, GetBookRequest]): Stream[F, Book]
      |}
      |
      |}""".stripMargin
  }

  implicit class StringOps(self: String) {
    def clean: String = self.replaceAll("\\s", "")
  }

}
