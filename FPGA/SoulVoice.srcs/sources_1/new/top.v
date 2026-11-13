`timescale 1ns / 1ps
/*
---- Top level module ----
16 switches
5 buttons, each with 1 debounce
ouput transmitter tx
1 byte per letter
*/

module top(
input wire clk,
input wire[4:0] btn,
input wire[7:0] sw,
output wire[7:0] led,
output wire tx
);
assign led = sw;

parameter MSG_DELAY = 150000000; // 100MHz/s => 1.5 sec = 1.5M
wire [4:0] btn_pressed;
wire uart_idle;
reg uart_start;
reg [7:0] uart_data;
reg msg_busy;
reg [3:0] sw_index; //8 switches + no switches
reg [2:0] btn_index; // 5 buttons
reg [5:0] char_index; // 38 max chars
wire [7:0] char_out; // 1 byte per char
wire [5:0] msg_length; // max 38 chars
reg [27:0] counter; // counts to 200.0000.000

initial begin 
    msg_busy = 0;
    uart_start = 0;
end

// debounes for all the buttons
debounce debounce0 (
.clk (clk),
.btn(btn[0]),
.pressed(btn_pressed[0])
);

debounce debounce1(
.clk (clk),
.btn(btn[1]),
.pressed(btn_pressed[1])
);

debounce debounce2 (
.clk (clk),
.btn(btn[2]),
.pressed(btn_pressed[2])
);

debounce debounce3 (
.clk (clk),
.btn(btn[3]),
.pressed(btn_pressed[3])
);

debounce debounce4 (
.clk (clk),
.btn(btn[4]),
.pressed(btn_pressed[4])
);

// uart transmitter unit
uart_tx uart_tx_u(
.clk(clk),
.send_signal(uart_start),
.data_in(uart_data),
.tx_pin(tx),
.is_idle(uart_idle)
);

// messages unit
message_rom message_rom_u(
.sw_index(sw_index),
.btn_index(btn_index),
.char_index(char_index),
.char_out(char_out),
.msg_length(msg_length)
);

always @(posedge clk) begin
    if(msg_busy == 0) begin
        if(btn_pressed != 0) begin            
            case (btn_pressed)
                5'b00001: btn_index <= 0;
                5'b00010: btn_index <= 1;
                5'b00100: btn_index <= 2;
                5'b01000: btn_index <= 3;
                5'b10000: btn_index <= 4;
                default: btn_index <= 0;
             endcase

             case (sw)
                8'b00000001: sw_index <= 0;
                8'b00000010: sw_index <= 1;
                8'b00000100: sw_index <= 2;
                8'b00001000: sw_index <= 3;
                8'b00010000: sw_index <= 4;
                8'b00100000: sw_index <= 5;
                8'b01000000: sw_index <= 6;
                8'b10000000: sw_index <= 7;
                8'b00000000: sw_index <= 8;
                default: sw_index <= 8;
             endcase            
             
             uart_start <=0;
             msg_busy <=1;        
             char_index <= 0;            
             end        
       end

    else begin 
       if(uart_idle == 1 && uart_start  == 0) begin
           if (char_index<msg_length) begin
            uart_data <= char_out;   
            uart_start <= 1; // send
            char_index <= char_index + 1;     
           end
           else begin //delay 2 sec
            if(counter < MSG_DELAY)
                counter <= counter + 1;
            else begin
                counter <= 0;
                msg_busy <=0; // done sending and delaying           
            end
          end    
       end       
       else uart_start <= 0; // wait
   end    
end
endmodule