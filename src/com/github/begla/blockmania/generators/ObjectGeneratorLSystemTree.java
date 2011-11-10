/*
 * Copyright 2011 Benjamin Glatzel <benjamin.glatzel@me.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.begla.blockmania.generators;

import com.github.begla.blockmania.blocks.BlockManager;
import com.github.begla.blockmania.world.LocalWorldProvider;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.util.Stack;

/**
 * Allows generation of complex trees based on Lindenmayer-Systems.
 *
 * @author Benjamin Glatzel <benjamin.glatzel@me.com>
 */
public class ObjectGeneratorLSystemTree extends ObjectGenerator {

    private static final int ITERATIONS = 5;

    public ObjectGeneratorLSystemTree(LocalWorldProvider w) {
        super(w);
    }

    /**
     * Generates the tree.
     *
     * @param posX Origin on the x-axis
     * @param posY Origin on the y-axis
     * @param posZ Origin on the z-axis
     */
    @Override
    public void generate(int posX, int posY, int posZ, boolean update) {

        String axiom = "F";

        Stack<Vector4f> _stackPosition = new Stack<Vector4f>();
        Stack<Matrix4f> _stackOrientation = new Stack<Matrix4f>();

        for (int i = 0; i < ITERATIONS; i++) {

            String temp = "";

            for (int j = 0; j < axiom.length(); j++) {
                char c = axiom.charAt(j);

                switch (c) {
                    case 'F':
                        temp += "GG[+F][**+F][//+F]";
                        continue;
                    case 'G':
                        temp += "GG";
                        continue;
                }

                temp += c;
            }

            axiom = temp;
        }

        Vector4f position = new Vector4f(0, 0, 0, 1);
        Matrix4f rotation = new Matrix4f();
        rotation.rotate((float) Math.PI / 2, new Vector3f(0, 0, 1));

        for (int i = 0; i < axiom.length(); i++) {
            char c = axiom.charAt(i);

            switch (c) {
                case 'G':
                case 'F':
                    byte blockType;

                    blockType = BlockManager.getInstance().getBlock("Tree trunk").getId();
                    _worldProvider.setBlock(posX + (int) position.x, posY + (int) position.y, posZ + (int) position.z, blockType, update, false);

                    blockType = BlockManager.getInstance().getBlock("Dark leaf").getId();

                    if (_stackOrientation.size() > 0) {
                        int size = 1;

                        for (int x = -size; x <= size; x++) {
                            for (int y = -size; y <= size; y++) {
                                for (int z = -size; z <= size; z++) {
                                    if (Math.abs(x) == size && Math.abs(y) == size && Math.abs(z) == size)
                                        continue;

                                    if (_worldProvider.getBlock(posX + (int) position.x + x, posY + (int) position.y + y, posZ + z +(int) position.z) == 0x0)
                                        _worldProvider.setBlock(posX + (int) position.x + x, posY + (int) position.y + y, posZ + z +(int) position.z , blockType, update, false);

                                }
                            }
                        }
                    }

                    Vector4f dir = new Vector4f(1, 0, 0, 1);
                    Matrix4f.transform(rotation, dir, dir);

                    position.x += dir.x;
                    position.y += dir.y;
                    position.z += dir.z;
                    break;
                case '[':
                    _stackOrientation.push(new Matrix4f(rotation));
                    _stackPosition.push(new Vector4f(position));
                    break;
                case ']':
                    rotation = _stackOrientation.pop();
                    position = _stackPosition.pop();
                    break;
                case '+':
                    rotation.rotate((float) (Math.PI / 3), new Vector3f(0, 0, 1));
                    break;
                case '-':
                    rotation.rotate((float) -(Math.PI / 3), new Vector3f(0, 0, 1));
                    break;
                case '&':
                    rotation.rotate((float) (Math.PI / 3), new Vector3f(0, 1, 0));
                    break;
                case '^':
                    rotation.rotate((float) -(Math.PI / 3), new Vector3f(0, 1, 0));
                    break;
                case '*':
                    rotation.rotate((float) (Math.PI / 3), new Vector3f(1, 0, 0));
                    break;
                case '/':
                    rotation.rotate((float) -(Math.PI / 3), new Vector3f(1, 0, 0));
                    break;
            }
        }
    }
}
