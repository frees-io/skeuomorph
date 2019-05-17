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

package higherkindness.skeuomorph
package uast

import iota.{CopK, TListK}
import cats._

object derivation {

  def copkTraverse[LL <: TListK](implicit M: TraverseMaterializer[LL]): Traverse[CopK[LL, ?]] =
    M.materialize(offset = 0)
  def delayEqCopK[LL <: TListK](implicit M: EqKMaterializer[LL]): Delay[Eq, CopK[LL, ?]] =
    M.materialize(offset = 0)
  def copkPrinter[LL <: TListK](implicit M: PrinterKMaterializer[LL]): Delay[Printer, CopK[LL, ?]] =
    M.materialize(offset = 0)

}
