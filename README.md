# Computer Simulator

## Introduction

This is a simulator for a computer, that is based on the Intel x86 architecture.

<span style="color: red;">**Note:**</span> This is not a simulator for the Intel x86 architecture.
The differences to the Intel x86 architecture will be explained in the respective sections.

## Instructions

### Intel x86 Comparison

I decided to implement instructions in a way that groups instructions based on their functionality.

#### Intel x86 Instructions

In general, instructions are composed of an opcode and operands (or arguments).

For Intel x86 instructions (in most cases), the opcode completely describes the operation except for
the operands, meaning that knowing the opcode tells you the length, the number of operands and the
types of the operands (e.g., register, memory, immediate).

#### Simulator Instructions

The opcode differentiates the instructions in the simulator, but they only determine the basic
operation.

Information about the operands is stored as additional data, after the opcode.
This also means that the opcode does not determine the length of the instruction.

### General Format for simple operands

Most operands are stored in at least two bytes.

The first byte contains the type of the operand and also the size of the value.

Depending on the type, the following bytes contain the value or more information about the operand.

#### Operand Types

##### Register

In this case, there is always only one byte following.

This byte contains the register number, as well as the region of the value.

The regions are:

- Low byte (8-bit)
- High byte (8-bit)
- Word (16-bit)
- Double word (32-bit)
- Quad word (64-bit)

It is also checked that the size of the register is compatible with the size of the value and that a
region is allowed for the used register.

##### Immediate

The immediate value is stored in the following 1, 2, 4 or 8 bytes, depending on the size of the
value.

##### Memory

The memory stores another byte of information that further specifies the way the address is
calculated as well as the memory segment.

The different types are:

- direct: constant value in qword (e.g., `mov rax, [0x1234567890abcdef]`)
- indirect: register in byte (e.g., `mov rax, [rbx]`)
- displacement: constant value in qword and register in byte (
  e.g., `mov rax, [rbx + 0x1234567890abcdef]`)
- indexed: register in byte and register in byte (e.g., `mov rax, [rbx + rsi]`)
- indexed with displacement: constant value in qword and two registers in two bytes (
  e.g., `mov rax, [rbx + rsi + 0x1234567890abcdef]`)
- scaled: register in byte and constant value in word (e.g., `mov rax, [rbx * 2]`)
- scaled with displacement: constant value in qword, register in byte and constant value in word (
  e.g., `mov rax, [rbx * 2 + 0x1234567890abcdef]`)
- scaled indexed: register in byte, register in byte and constant value in word (
  e.g., `mov rax, [rbx + rsi * 2]`)
- scaled indexed with displacement: constant value in qword, two registers in two bytes and constant
  value in word (e.g., `mov rax, [rbx + rsi * 2 + 0x1234567890abcdef]`)

Important to note is that the assembler requires the exact order as above and only scaled registers and
constant values can be subtracted, registers that are not scaled only allow addition, meaning
`[rbx - rsi * 2]` is allowed, but `[-rbx + rsi * 2]` or `[rbx - rax]` is not.
