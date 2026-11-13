`timescale 1ns / 1ps
/*
---- Button debounce logic ----
checks if a button was pressed by checking if the 
received values was high for 10ms

100Mhz * 10ms = 1.000.000 ticks of the clock 
2^20 = 1.048.576 => needs 20 bits register
*/

module debounce(
    input wire clk,
    input wire btn,
    output wire pressed
    );

reg[19:0] count = 20'b0;
reg pressed_reg;
assign pressed = pressed_reg;

initial begin
pressed_reg = 0;
count= 0;
end
always @(posedge clk) begin
    if(count < 1000000) begin
        if(btn == 1) 
            count <= count + 1;
        else count <= 0;
        pressed_reg <= 0;
    end
    else begin
        count <= 0;
        pressed_reg <= 1;
    end
end
    
endmodule
