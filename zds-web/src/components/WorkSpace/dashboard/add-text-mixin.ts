import { Vue, Component } from 'vue-property-decorator';
import $ from 'jquery';
import { WorkspaceComponentBase, Rectangle } from '@/types/workspace';
@Component
export default class AddTextMixin extends WorkspaceComponentBase {
	addTextMode = false;
	adding = false;
	rectangle: Rectangle;
	beforeMount() {
		this.rectangle = new Rectangle();
	}

	get $rectangle() {
		return $(this.$refs['selectedRectangle']);
	}
	get $layoutContainer() {
		return $(this.$refs['layoutContainer']);
	}
	rerendRectangle() {
		const $rectangle = this.$rectangle;
		if ($rectangle) {
			$rectangle.css('left', this.rectangle.left);
			$rectangle.width(this.rectangle.width);
			$rectangle.css('top', this.rectangle.top);
			$rectangle.height(this.rectangle.height);
		}
	}
	containerMouseDown(e: MouseEvent) {
		if (!this.addTextMode) {
			return;
		}
		this.adding = true;
		this.rectangle = new Rectangle(e.offsetX, e.offsetY);
		this.rerendRectangle();
	}
	containerMouseUp(e: MouseEvent) {
		if (!this.addTextMode) {
			return;
		}

		this.adding = false;
		this.addText(this.rectangle);
	}
	containerMouseMove(e: MouseEvent) {
		if (!this.addTextMode) {
			return;
		}
		if (this.adding) {
			const scrollTop = this.$layoutContainer.scrollTop() || 0;
			let offsetX = this.getContainerOffset(e).left;
			let offsetY = this.getContainerOffset(e).top + scrollTop;
			const width = offsetX - this.rectangle.x;
			const height = offsetY - this.rectangle.y;
			this.rectangle.onDrag(width, height);
			this.rerendRectangle();
		}
	}
	getContainerOffset(e: MouseEvent) {
		if (e.target) {
			let $target = $(e.target);
			let offsetX = e.offsetX,
				offsetY = e.offsetY;
			while (!$target.hasClass('layout-container')) {
				let offset = $target.position();
				if (offset) {
					offsetY += offset.top;
					offsetX += offset.left;
					$target = $target.parent();
				} else {
					break;
				}
			}
			return {
				top: offsetY,
				left: offsetX
			};
		}
		return {
			top: 0,
			left: 0
		};
	}
	addText(rect: Rectangle) {
	}
}
